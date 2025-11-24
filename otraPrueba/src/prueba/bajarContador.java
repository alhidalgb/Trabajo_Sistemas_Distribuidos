package prueba;

import java.util.concurrent.CountDownLatch;

public class bajarContador implements Runnable{

	
	private CountDownLatch count;
	
	public bajarContador(CountDownLatch count) {
		
		this.count=count;
		
		
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		this.count.countDown();
		
	}

}
