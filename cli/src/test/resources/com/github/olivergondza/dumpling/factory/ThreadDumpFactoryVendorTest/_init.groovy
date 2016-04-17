def pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().replaceAll(/@.*/, '');
def writer = new java.io.PrintWriter(D.args[0]);
writer.println(pid);
writer.close();

def pauseHere() {
    def o = new Object();
    synchronized (o) {
        while (true) {
            o.wait();
        }
    }
}

/// End of _init
