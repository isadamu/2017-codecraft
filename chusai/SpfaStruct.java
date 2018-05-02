package new6;

/**
 * SPFA标记类
 * @author 12896
 *
 */
public class SpfaStruct {
	
	public Edge pre_edge;           //它的前向边
	
	public int distance;            //距离
	
	public int begin_node;          //这条路径的起始点
	
	public void set( Edge pre_edge, int distance, int begin_node) {  //设置
		this.pre_edge = pre_edge;
		this.distance = distance;
		this.begin_node = begin_node;
	}
	
	public void reInit() {  //初始化
		this.pre_edge = null;
		this.distance = MyAlgorithmForUp.MY_MAX;
		this.begin_node = -1;
	}
}
