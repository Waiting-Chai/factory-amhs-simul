package com.semi.jSimul.core;

/**
 * Internal helper to create an initialization event for a process (composition).
 * Returns a preconfigured Event (ok=true, value=null) with a callback to proc::_resume.
 *
 * @author waiting
 * @date 2025/10/29
 */
class Initialize {

    static Event make(Environment env, Process proc) {
        Event e = new Event(env);
        e.addCallback(proc::_resume);
        e.markOk(null);
        return e;
    }

}