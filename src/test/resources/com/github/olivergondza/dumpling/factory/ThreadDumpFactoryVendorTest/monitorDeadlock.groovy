def monitorA = new Object();
def monitorB = new Object();

def other = new Thread("other") {
    def void run() {
        synchronized(monitorA) {
            synchronized(monitorB) {
                
            }
        }
    }    
}

synchronized(monitorB) {
    other.start();
    Thread.sleep(10);
    synchronized (monitorA) {
        
    }
}
