package last7;

/**
 * 边 u->v  
 * @author 12896
 *
 */
public class Edge {
	
	public boolean is_in_tree;   //是否在树中
	
	public boolean is_forward;   //在树中属于正向还是反向
	
	public int pre;       //u
	
	public int next;      //v
	
	public int cap;       //容量
	
	public int price;     //单价
	
	public int flow;      //流量
	
	public int for_find_path;
	
	public Edge tree_pre_edge;
	
	public Edge Tree_next_head;	
	
	public Edge brother_pre;	
	
	public Edge brother_next;	
	
	
	public Edge(int pre, int next, int cap, int price, int flow) {
		super();
		this.pre = pre;
		this.next = next;
		this.cap = cap;
		this.price = price;
		this.flow = flow;
	}
	
}
