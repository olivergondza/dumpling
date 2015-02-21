import java.util.concurrent.locks.LockSupport;

for (;;) {
    LockSupport.park();
}
