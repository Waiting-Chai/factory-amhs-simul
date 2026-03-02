package com.semi.jSimul.examples;

import com.semi.jSimul.collections.Request;
import com.semi.jSimul.collections.Resource;
import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Process;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A full-day flow-line simulation: jobs must pass stations A->B->C->D->E->F.
 * Each station has one machine; vehicles move parts between stations at a fixed speed.
 *
 * <p>Run the main method to sweep vehicle counts and find the smallest fleet size that
 * can meet the target daily throughput. Logging is used to expose major milestones
 * and can be tuned by adjusting the global JUL level.
 *
 * @author waiting
 * @date 2025/12/02
 */
public final class FlowLineScenario {

    private static final double AREA_SIDE = Math.sqrt(1000.0); // meters (square layout)
    private static final double DAY_SECONDS = 24 * 3600.0;
    private static final Logger LOG = Logger.getLogger(FlowLineScenario.class.getName());

    private record Station(String name, double x, double y, double processTime, Resource machine) {
    }

    public record Params(int dailyTarget, double vehicleSpeedMps) {
    }

    public record Stats(int completed, double avgFlowTime, double makespan) {
    }

    public static void main(String[] args) {
        int daily = 200;
        double speed = 1.2;
        int maxVehicles = 10;
        boolean verbose = true;
        if (args != null) {
            if (args.length >= 1) daily = Integer.parseInt(args[0]);
            if (args.length >= 2) speed = Double.parseDouble(args[1]);
            if (args.length >= 3) maxVehicles = Integer.parseInt(args[2]);
            if (args.length >= 4) verbose = Boolean.parseBoolean(args[3]);
        }
        Params params = new Params(daily, speed);
        int vehicles = findMinimumVehicles(params, maxVehicles, verbose);
        LOG.info(() -> String.format("Minimum vehicles to hit %,d jobs/day at %.2f m/s: %d",
                params.dailyTarget(), params.vehicleSpeedMps(), vehicles));
    }

    /**
     * Sweep vehicle counts starting from 1 until the target throughput is met or maxVehicles is reached.
     */
    public static int findMinimumVehicles(Params params, int maxVehicles, boolean verbose) {
        for (int v = 1; v <= maxVehicles; v++) {
            Stats stats = runOnce(params, v, verbose);
            int finalV = v;
            LOG.info(() -> String.format("[vehicles=%d] completed=%d avgFlowTime=%.1fs makespan=%.1fs",
                    finalV, stats.completed(), stats.avgFlowTime(), stats.makespan()));
            if (stats.completed() >= params.dailyTarget()) {
                return v;
            }
        }
        return maxVehicles;
    }

    /**
     * Run a single-day simulation with a fixed vehicle count.
     *
     * @param params        throughput and speed parameters
     * @param vehicles      fleet size to simulate
     * @param verboseEvents if true, logs per-job milestones
     */
    public static Stats runOnce(Params params, int vehicles, boolean verboseEvents) {
        Environment env = new Environment();

        List<Station> stations = List.of(
                makeStation(env, "A", 0.1 * AREA_SIDE, 0.1 * AREA_SIDE, 20.0),
                makeStation(env, "B", 0.3 * AREA_SIDE, 0.2 * AREA_SIDE, 30.0),
                makeStation(env, "C", 0.5 * AREA_SIDE, 0.4 * AREA_SIDE, 25.0),
                makeStation(env, "D", 0.7 * AREA_SIDE, 0.6 * AREA_SIDE, 18.0),
                makeStation(env, "E", 0.8 * AREA_SIDE, 0.3 * AREA_SIDE, 22.0),
                makeStation(env, "F", 0.9 * AREA_SIDE, 0.8 * AREA_SIDE, 15.0)
        );

        Resource vehiclePool = new Resource(env, vehicles);

        AtomicInteger completed = new AtomicInteger();
        DoubleAdder totalFlow = new DoubleAdder();

        double interArrival = DAY_SECONDS / params.dailyTarget();
        LOG.log(Level.INFO, "Starting simulation: target={0} jobs, vehicles={1}, speed={2} m/s, interArrival={3}s",
                new Object[]{params.dailyTarget(), vehicles, params.vehicleSpeedMps(), interArrival});

        for (int i = 0; i < params.dailyTarget(); i++) {
            final double releaseAt = i * interArrival;
            env.process(jobProcess(env, vehiclePool, stations, params.vehicleSpeedMps(),
                    i, releaseAt, completed, totalFlow, verboseEvents));
        }

        env.run(DAY_SECONDS);

        int done = completed.get();
        double avgFlow = done == 0 ? 0.0 : totalFlow.doubleValue() / done;
        double makespan = env.now();
        LOG.log(Level.INFO, "Simulation done: completed={0}, avgFlow={1}s, makespan={2}s",
                new Object[]{done, avgFlow, makespan});
        return new Stats(done, avgFlow, makespan);
    }

    private static Station makeStation(Environment env, String name, double x, double y, double processTime) {
        return new Station(name, x, y, processTime, new Resource(env, 1));
    }

    private static Process.ProcessFunction jobProcess(Environment env,
                                                      Resource vehicles,
                                                      List<Station> stations,
                                                      double speedMps,
                                                      int jobId,
                                                      double releaseAt,
                                                      AtomicInteger completed,
                                                      DoubleAdder totalFlow,
                                                      boolean verbose) {
        return ctx -> {
            ctx.await(env.timeout(Math.max(0.0, releaseAt - env.now())));
            if (verbose) {
                LOG.info(String.format("[t=%.1f] job-%d released", env.now(), jobId));
            }
            double start = env.now();
            double currentX = 0.0;
            double currentY = 0.0;
            for (Station st : stations) {
                Request vehReq = vehicles.request();
                ctx.await(vehReq.asEvent());
                double travel = travelTime(currentX, currentY, st.x(), st.y(), speedMps);
                if (verbose) {
                    LOG.info(String.format("[t=%.1f] job-%d moving to %s, travel=%.1fs",
                            env.now(), jobId, st.name(), travel));
                }
                ctx.await(env.timeout(travel));
                vehicles.release(vehReq).asEvent();

                Request machReq = st.machine().request();
                ctx.await(machReq.asEvent());
                if (verbose) {
                    LOG.info(String.format("[t=%.1f] job-%d processing %s, duration=%.1fs",
                            env.now(), jobId, st.name(), st.processTime()));
                }
                ctx.await(env.timeout(st.processTime()));
                st.machine().release(machReq).asEvent();

                currentX = st.x();
                currentY = st.y();
            }
            double flow = env.now() - start;
            completed.incrementAndGet();
            totalFlow.add(flow);
            if (verbose) {
                LOG.info(String.format("[t=%.1f] job-%d done, flowTime=%.1fs", env.now(), jobId, flow));
            }
            return "done";
        };
    }

    private static double travelTime(double x1, double y1, double x2, double y2, double speedMps) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dist = Math.hypot(dx, dy);
        if (speedMps <= 0) {
            throw new IllegalArgumentException("vehicle speed must be > 0");
        }
        return dist / speedMps;
    }
}
