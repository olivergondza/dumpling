def a = new Object();
def b = new Object();
def c = new Object();

def other = new Thread("reacquiring") {
    def void run() {
        synchronized (a) {
            synchronized (b) {
                synchronized (c) {
                    synchronized (b) {
                        synchronized (a) {
                            a.wait();
                        }
                    }
                }
            }
        }
    }
}

other.start();
Thread.sleep(100);

synchronized (a) {
    a.notify();
    
    // Block forever
    Thread.sleep(60000);
}