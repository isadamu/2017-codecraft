package new6;

public class Deploy  // 3-29
{
	
	public static void main( String[] args ) {
		FileUtil.write( "F:\\我的坚果云\\hwrj\\hwrj\\result_new2.txt", Deploy.deployServer( 
				FileUtil.read( "D:\\workspace\\hwrj\\src\\hwrj\\testfile\\new\\gao0.txt", null) ), false);
	}
    /**
     * 你需要完成的入口
     * 完成服务器部署
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */
    public static String[] deployServer(String[] graphContent)
    {	
    	MyAlgorithmForUp mafu = new MyAlgorithmForUp();
		return mafu.algorithmRun(graphContent);
	}

}
