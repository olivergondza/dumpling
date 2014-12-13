def a = new Object ();
def b = new Integer(42);

synchronized (a) {
    synchronized (b) {
        a.wait();
    }
}
