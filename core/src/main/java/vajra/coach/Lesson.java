package vajra.coach;

/** One thing a pilot must be taught: what it is called, the keys that do it, what it does, and
 *  the moment the world should first teach it. Pure data - the page and the in-run coach both
 *  read the same sheet. */
public final class Lesson {
    public final int id;
    public final String group, title;
    public final String[] keys;   // key caps; "/" between caps reads as "or"
    public final String[] body;   // up to four short lines
    public final String when;     // the first time this matters

    Lesson(int id, String group, String title, String[] keys, String[] body, String when) {
        this.id = id;
        this.group = group;
        this.title = title;
        this.keys = keys;
        this.body = body;
        this.when = when;
    }
}
