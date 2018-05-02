package new6;

/**
 * 边 u->v  
 * @author 12896
 *
 */
public class Edge {
	
	public int pre;       //u
	
	public int next;      //v
	
	public int cap;       //容量
	
	public int price;     //单价
	
	public int flow;      //流量
	
	public int spfa_1;    //SPFA边权，由于在SPFA中，一个方向有两条边，所以有两个权重
	
	public int spfa_2;    //SPFA边权
	
	public boolean use_1;     //当前使用的哪个权重
	
	public Edge edge_re;   //反向边 v->u
	
	public Edge(int pre, int next, int cap, int price, int flow, int spfa_1, int spfa_2, boolean use_1, Edge edge_re) {
		super();
		this.pre = pre;
		this.next = next;
		this.cap = cap;
		this.price = price;
		this.flow = flow;
		this.spfa_1 = spfa_1;
		this.spfa_2 = spfa_2;
		this.use_1 = use_1;
		this.edge_re = edge_re;
	}
	
	public void reInit() {
		
		this.flow = 0;
		this.spfa_1 = this.price;
		this.spfa_2 = MyAlgorithmForUp.MY_MAX;
		this.use_1 = true;
	}
	
}
