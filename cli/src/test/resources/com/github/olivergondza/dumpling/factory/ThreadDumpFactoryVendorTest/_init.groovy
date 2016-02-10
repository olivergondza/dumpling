def pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().replaceAll(/@.*/, '');
def writer = new java.io.PrintWriter(D.args[0]);
writer.println(pid);
writer.close();

/// End of _init
