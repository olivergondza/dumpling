def pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().replaceAll(/@.*/, '');
def file = new File(D.args[0]);
if (file.exists()) file.delete();
file << "${pid}\n";

/// End of _init
