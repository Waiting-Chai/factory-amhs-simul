package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 模拟单辆 AGV 从 A 经 B/C/D/E/F 到 G 的搬运过程。
 */
public class AgvProcessTest {

    @Test
    void agvTransportJourney() {
        Environment env = new Environment();
        List<String> timeline = new ArrayList<>();

        AgvWorkflow workflow = new AgvWorkflow(env, "A", timeline);
        workflow
                .travelTo("B", 1.0)
                .operate("loaded", 2.0)
                .travelTo("C", 1.0)
                .travelTo("D", 1.0)
                .travelTo("E", 1.0)
                .travelTo("F", 1.0)
                .travelTo("G", 1.0)
                .operate("unloaded", 2.0);

        Process agv = workflow.start();

        Object ret = env.run(agv);
        assertEquals("AGV-A->G", ret);
        assertEquals(10.0, env.now(), 1e-9);
        assertEquals(
                List.of(
                        "B@1.0", "loaded@3.0", "C@4.0", "D@5.0", "E@6.0", "F@7.0", "G@8.0", "unloaded@10.0"),
                timeline);
    }

    /**
     * 通过链式 step 构建 AGV 搬运流程，可按需动态追加步骤。
     */
    private static final class AgvWorkflow {

        @FunctionalInterface
        private interface Step {
            void perform(Process.ProcessContext ctx) throws Exception;
        }

        private final Environment env;
        private final List<String> timeline;
        private final List<Step> steps = new ArrayList<>();
        private final String startPoint;
        private String current;

        AgvWorkflow(Environment env, String startPoint, List<String> timeline) {
            this.env = env;
            this.startPoint = startPoint;
            this.current = startPoint;
            this.timeline = timeline;
        }

        AgvWorkflow travelTo(String nextPoint, double travelSeconds) {
            steps.add(
                    ctx -> {
                        ctx.await(new Timeout(ctx.env(), travelSeconds));
                        timeline.add(nextPoint + "@" + ctx.env().now());
                    });
            this.current = nextPoint;
            return this;
        }

        AgvWorkflow operate(String label, double seconds) {
            steps.add(
                    ctx -> {
                        ctx.await(new Timeout(ctx.env(), seconds));
                        timeline.add(label + "@" + ctx.env().now());
                    });
            return this;
        }

        Process start() {
            final String endPoint = this.current;
            return new Process(
                    env,
                    ctx -> {
                        for (Step step : steps) {
                            step.perform(ctx);
                        }
                        return "AGV-" + startPoint + "->" + endPoint;
                    });
        }
    }
}
