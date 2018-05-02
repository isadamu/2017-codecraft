package last7;


public class MyLastMain7 { //更换使用了网络单纯形
	public static void main( String[] args ) {  
				
		for ( int i = 0 ; i < 10 ; i++ ) {
			MyLastAlgorithm7 nt = new MyLastAlgorithm7();
			nt.testfile = 
					"F:\\我的坚果云\\hwrj\\复赛\\case_example\\zh_end" + i + ".txt";
			
			String[] data = FileUtil.read( nt.testfile, null);   //读文件
			
			String[] result = nt.algorithmRun(data);
			
			String[] debug = nt.debug;
			
			FileUtil.write( "F:\\我的坚果云\\hwrj\\复赛\\result_wakaka.txt", result, false);	
			
			FileUtil.write( "F:\\我的坚果云\\hwrj\\复赛\\debug_test15.txt", debug, true);	
		}
		
		for ( int i = 0 ; i < 10 ; i++ ) {
			MyLastAlgorithm7 nt = new MyLastAlgorithm7();
			nt.testfile = 
					"F:\\我的坚果云\\hwrj\\复赛\\case_example\\g_end" + i + ".txt";
			
			String[] data = FileUtil.read( nt.testfile, null);   //读文件
			
			String[] result = nt.algorithmRun(data);
			
			String[] debug = nt.debug;
			
			FileUtil.write( "F:\\我的坚果云\\hwrj\\复赛\\result_wakaka.txt", result, false);	
			
			FileUtil.write( "F:\\我的坚果云\\hwrj\\复赛\\debug_test15.txt", debug, true);	
		}

	}
}
