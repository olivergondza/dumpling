import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.*;

def cdl = new CountDownLatch(1);

def lockA = new ReentrantLock();
def lockB = new ReentrantLock();

def other = new Thread("other") {
    def void run() {
        lockA.lock();
        cdl.countDown();
        lockB.lock();
    }
}

lockB.lock();
other.start();
cdl.await();
lockA.lock();
