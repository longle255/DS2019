import java.util.Random;

public class HelloThread {
    class MyThread1 extends Thread {
        private int id;

        public MyThread1(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            System.out.println("Thread " + id + " start");
//            for (int i = 0; i<1000000000; i++){}
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread " + id + " stop");
        }
    }

    class MyThread2 implements Runnable {

        private int id;

        public MyThread2(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            System.out.println("Thread " + id + " start");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread " + id + " stop");
        }
    }

    public void hello(){
        Thread thread1 = new MyThread1(1);
        Runnable thread2 = new MyThread2(2);

        thread1.start();
        thread2.run();
    }

    public static void main(String args[]){
        HelloThread app = new HelloThread();
        app.hello();
    }
}
