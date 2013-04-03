import java.util.concurrent.LinkedBlockingQueue;

public class SharedQueue<T> {
     //Creating shared object
    private LinkedBlockingQueue<T> sharedQueue;
    SharedQueue(int c){
    	sharedQueue = new LinkedBlockingQueue<T>(c);
    }
    public void insert(T t){
    	try {
            sharedQueue.put(t);
        } catch (InterruptedException e) {
			System.err.println(e);        }
    }
    public T remove(){
    	T t = null;
    	try {
            t = sharedQueue.take();
        } catch (InterruptedException e) {
			System.err.println(e);        }
		return t;
    }
}