import java.util.concurrent.locks.LockSupport;

a = new Object();
b = new Integer(42);
c = new Double(3.14);

def bar() {
    LockSupport.park();
}

def foo() {
    synchronized (a) { synchronized (b) { synchronized (b) {synchronized (c) {
       bar(); 
    }}}}
}

foo();
