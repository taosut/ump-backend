/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt;

import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 * @author kiendt
 */
public class ThreadPoolMonitor implements Runnable {

    private ThreadPoolExecutor executor;
    private int seconds;
    private boolean run = true;

    public ThreadPoolMonitor(ThreadPoolExecutor executor, int delay) {
        this.executor = executor;
        this.seconds = delay;
    }

    public void shutdown() {
        this.run = false;
    }

    public void run() {
        while (run) {
            System.out
                    .println(String
                            .format("[monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s, Queue.size: %d",
                                    this.executor.getPoolSize(), this.executor.getCorePoolSize(),
                                    this.executor.getActiveCount(), this.executor.getCompletedTaskCount(),
                                    this.executor.getTaskCount(), this.executor.isShutdown(),
                                    this.executor.isTerminated(), this.executor.getQueue().size()));

            try {
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
