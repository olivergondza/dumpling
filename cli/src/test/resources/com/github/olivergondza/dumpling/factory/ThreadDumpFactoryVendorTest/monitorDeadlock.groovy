import java.util.concurrent.CountDownLatch

def cdl = new CountDownLatch(1);

def monitorA = new Object();
def monitorB = new Object();

def other = new Thread("other") {
    def void run() {
        synchronized(monitorA) {
            cdl.countDown();
            synchronized(monitorB) {
            }
        }
    }    
}

synchronized(monitorB) {
    other.start();
    cdl.await();
    synchronized (monitorA) {
        
    }
}
