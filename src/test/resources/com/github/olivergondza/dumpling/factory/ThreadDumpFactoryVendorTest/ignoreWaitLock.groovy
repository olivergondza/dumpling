def a = new Double(3.14);
def b = new Integer(42);

synchronized (a) {
    synchronized (b) {
        a.wait();
    }
}
