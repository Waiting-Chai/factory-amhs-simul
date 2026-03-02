package com.semi.jSimul.core;

/**
 * Internal helper to create an interruption event for a process (composition).
 * Returns a preconfigured Event (ok=false, defused=true, value=Interrupt).
 */
class Interruption {

    static Event make(Process proc, Object cause) {
        Event e = new Event(proc.env());
        e.addCallback(proc::_resume);
        e.ok = false;
        e.defused = true; // prevent environment crash
        e.value = new Interrupt(cause);
        return e;
    }

}