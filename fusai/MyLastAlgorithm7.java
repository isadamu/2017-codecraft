package last7;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class MyLastAlgorithm7 {           //核心已经更换为网络单纯形 
	
	public String testfile = "";
	
	public String[] debug = null;
	
	private int MCMF_COUNT = 0;
	
	private final int MOVE_FACT = 3;                 //加入移动因子
	
	private final int EX_FACT = 3;                 //加入交换因子
	
	private static final int MY_MAX = 12345678;       //表示正无穷
	
	private static final int PRICE_MAX = 10000;       //表示单位费用无穷
	
	private int time_limit = 48000;       //用时上限
	
	private ArrayList<HashMap<Integer,Edge>> digraph_map = new ArrayList<>();  //存储有向图 ，用于计算
	
	private ArrayList<HashMap<Integer,Edge>> digraph_map_best = new ArrayList<>();  //存储最好的流结果，和上面的图结构一致
	
	private Edge[] T0_edge = null;
	
	private ArrayList<Edge> all_edges = new ArrayList<>();
	
	private int node_num = 0;               //节点数（不包含超级源点超级汇点）
	
	private int edge_num = 0;               //总的边数
	
	private int tar_node_num = 0;           //tar表示目标，需求点个数
	
	private int[][] server_ability_cost = new int[100][2];        //服务器级别 能力加费用
	
	private int biggest_rank = -1;        //服务器级别 能力加费用
	
	private int[] deploy_cost = null;        //节点部署服务器的费用
	
	private long begin_time = 0;            //记录开始时间
	
	private HashMap<Integer,Integer> tars_need = new HashMap<>();  //需求点以及它的需求
	
	private HashMap<Integer,Integer> tars_real_number = new HashMap<>(); //需求点在图上的位置 与它实际的ID
	
	private HashMap<Integer,Integer> srcs_now = new HashMap<>(); //调整布置的源点
	
	private int result_cost_best = MY_MAX;                    //存储最低的花费
	
	private int result_cost_now = MY_MAX;               //存储 临时花费
	
	private int total_flow_best = 0;                             //存储 最大流量
	
	private int total_flow_now = 0;                             //存储 临时流量
	 
	private int total_node_need = 0;       //总需求
	
	private int super_src = -1;                             //超级源点
	
	private int super_tar = -1;                             //超级汇点
	
	private int d0 = -1;                             //超级源点
	
	private int[] pai = null;
	
	private boolean[] is_server = null;
	
	private boolean[] is_tar = null;
	
	private Edge[] node_pre_edge = null;
	
	private boolean need_find_src = true;
	
	private ArrayList<HashMap<Integer,Integer>> tar_srcs = null;
	
	private Random myrand = new Random();
	
	/**
	 * 算法入口，新策略，按照性价比部署服务器，（第一次不随机版本）（无升级因子）
	 */
	public String[] algorithmRun(String[] data) { 
		begin_time = System.currentTimeMillis();//开始计时
		algorithmInit(data);
		
		/************************* 1.每个点部署一个服务器，最大rank，看是否有解 *****************/
		for ( int node_index = 0 ; node_index < node_num ; node_index++ ) {
			is_server[node_index] = true;
			buildEdge( digraph_map, node_index, -1 , biggest_rank );
			buildEdge( digraph_map_best, node_index, -1, biggest_rank );
			srcs_now.put(node_index, biggest_rank);
		}
		
		networkSimplex();  //计算最小费用最大流
		
		if ( total_flow_now < total_node_need ) {  //无解
			System.out.println("NA");
			String[] arr_to_write = new String[1];
			arr_to_write[0] = "NA";
			return arr_to_write;
		}
			
		long time_count = 0;	
		int[] node_queue = new int[node_num]; 
		int node_queue_index = 0;
		/*************** 原始输入顺序 ********************/
		for ( int index = 0 ; index < node_num ; index++ ) {  
			node_queue[index] = index;
		}
		int cost_best_best = MY_MAX;
		HashMap<Integer,Integer> srcs_best_best = new HashMap<>();
		
		/***************************** 迭代三轮，防止随机波动(也就只能针对小图) *****************/
		for ( int turn = 1 ; turn <= 1 ; turn++ ) {
			time_count = System.currentTimeMillis();
			if ( time_count - begin_time >  time_limit ) {
				break;
			}
			srcs_now.clear();
			digraph_map.get(super_src).clear();
			digraph_map_best.get(super_src).clear();
			/************************* 每个点部署一个服务器，最大rank *****************/
			for ( int node_index = 0 ; node_index < node_num ; node_index++ ) {
				is_server[node_index] = true;
				buildEdge( digraph_map, node_index, -1 , biggest_rank );
				buildEdge( digraph_map_best, node_index, -1, biggest_rank );
				srcs_now.put(node_index, biggest_rank);
			}
			networkSimplex();  //计算最小费用最大流
			/******************* 将流量为0的服务器全删了，全部换成最优等级 *****************/
			Collection<Edge> sup_edge = new ArrayList<>();
			sup_edge.addAll( digraph_map.get(super_src).values() );
			for ( Edge edge : sup_edge ) {
				if ( edge.flow == 0 ) {
					buildEdge( digraph_map, -1, edge.next , -1 );
					buildEdge( digraph_map_best, -1, edge.next , -1 );
					srcs_now.remove(edge.next);
					is_server[edge.next] = false;
				} else {
					int best_rank = getBestRank(edge.next);  //更换为性价比最好的服务器
					digraph_map.get(super_src).get(edge.next).cap = server_ability_cost[best_rank][0];
					digraph_map_best.get(super_src).get(edge.next).cap = server_ability_cost[best_rank][0];
					srcs_now.put( edge.next, best_rank);
				}
			}
			networkSimplex();
			update_best();   //更新初始可行解
			int node;
			int count = 0;
			node_queue_index = 0;
			while ( count < node_queue.length ) {
				node = node_queue[node_queue_index];
				time_count = System.currentTimeMillis();
				if ( time_count - begin_time >  time_limit ) {
					break;
				}
				if ( is_server[node] ) {  //关联了服务器
					boolean had_success = false;
					if ( !had_success ) { //操作1: 尝试删除
						had_success = tryDelete( node);
					}
					if ( !had_success ) {
						int move_node;
						if ( srcs_now.get(node) == getBestRank(node) ) {
							move_node = tryMove( node, true);
						} else {
							move_node = tryMove( node, false);
						}
						if ( move_node != -1 ) {
							had_success = true;
						} 
					}
					if ( !had_success && srcs_now.get(node) != 0 ) {//操作3: 尝试降级 
						had_success = tryDownRank( node, false);
					}
					if ( had_success ) {
						count = 0;
					}
				} 
				count++;
				node_queue_index = (node_queue_index + 1) % node_queue.length ;
			}
			count = 0;
			node_queue_index = 0;
			need_find_src = true;
			while ( count < node_queue.length ) {
				node = node_queue[node_queue_index];
				time_count = System.currentTimeMillis();
				if ( time_count - begin_time >  time_limit ) {
					break;
				}
				if ( !is_server[node] ) {  //关联了服务器
					if ( tryExchange(node) ) {
						need_find_src = true;
					}
				} 
				count++;
				node_queue_index = (node_queue_index + 1) % node_queue.length ;
			}
			if ( cost_best_best > result_cost_best ) {
				cost_best_best = result_cost_best;
				srcs_best_best.clear();
				srcs_best_best.putAll(srcs_now);
			} 
			System.out.println("第" + turn + "轮，最优：" + cost_best_best + "	本轮最优：" + result_cost_best);
			
		}
		
		/******************** 如果是小图，则进一步操作 *********************/
		if ( node_num < 800 ) {
			/****************** 随机提升服务器等级 *******************/
			while ( true ) {
				time_count = System.currentTimeMillis();
				if ( time_count - begin_time >  time_limit ) {
					break;
				}
				/**************** 只提升部分服务器等级 ******************/
				digraph_map.get(super_src).clear();
				digraph_map_best.get(super_src).clear();
				for ( int node = 0 ; node < node_num ; node++ ) {
					is_server[node] = false;
				}
				srcs_now.clear();
				for ( int src : srcs_best_best.keySet() ) {
					int rank = srcs_best_best.get(src);
					if ( rank < biggest_rank ) {
						rank++;
					}
					srcs_now.put( src, rank);
				}
				
				for ( int src : srcs_now.keySet() ) {
					is_server[src] = true;
					buildEdge( digraph_map, src, -1, srcs_now.get(src) );
					buildEdge( digraph_map_best, src, -1, srcs_now.get(src) );
				}	
				networkSimplex();
				update_best();
				int rand_index = 0;
				int exchange_num = 0;
				for ( int index = 0 ; index < node_num ; index++ ) {  //随机交换，打乱一下原始顺序
					rand_index = myrand.nextInt(node_num);
					exchange_num = node_queue[index];
					node_queue[index] = node_queue[rand_index];
					node_queue[rand_index] = exchange_num;
				}
				int node;
				int count = 0;
				node_queue_index = 0;
				need_find_src = true;
				while ( count < node_queue.length ) {
					node = node_queue[node_queue_index];
					time_count = System.currentTimeMillis();
					if ( time_count - begin_time >  time_limit ) {
						break;
					}
					if ( is_server[node] ) {  //关联了服务器
						boolean had_success = false;

						if ( !had_success ) {
							int move_node;
							if ( srcs_now.get(node) == getBestRank(node) ) {
								move_node = tryMove( node, true);
							} else {
								move_node = tryMove( node, false);
							}
							while ( move_node != -1 ) {
								had_success = true;
								if ( srcs_now.get(move_node) == getBestRank(move_node) ) {
									move_node = tryMove( move_node, true);
								} else {
									move_node = tryMove( move_node, false);
								}
							}
						}
						if ( !had_success && srcs_now.get(node) != 0 ) {//操作3: 尝试降级 
							had_success = tryDownRank( node, false);
							if ( srcs_now.get(node) == 0 ) { //操作1: 尝试删除
								tryDelete( node);
							}
						}
						if ( !had_success  ) { //操作1: 尝试删除
							had_success = tryDelete( node);
						}
						if ( had_success ) {
							count = 0;
							need_find_src = true;
						}
					} else {
						if ( tryExchange(node) ) {
							need_find_src = true;
							count = 0;
						}
					}
					count++;
					node_queue_index = (node_queue_index + 1) % node_queue.length ;
				}
				if ( cost_best_best > result_cost_best ) {
					cost_best_best = result_cost_best;
					srcs_best_best.clear();
					srcs_best_best.putAll(srcs_now);
				} 
				System.out.println("最优：" + cost_best_best + "	本轮最优：" + result_cost_best);
			}
		}
		/********************** 用得到的最优解再计算一次 ********************/
		digraph_map.get(super_src).clear();
		for ( int src : srcs_best_best.keySet() ) {
			buildEdge( digraph_map, src, -1, srcs_best_best.get(src) );
		}	
		srcs_now.clear();
		srcs_now.putAll(srcs_best_best);
		networkSimplex();  //计算最小费用最大流
		update_best();   //更新初始可行解	
		/*******************************************************************/
		/*************************** 返回输出最优结果 ********************************/
		String str = "";
		String str_rank = "";
		for ( int src : srcs_now.keySet() ) {
			str += src + ", ";
			str_rank += srcs_now.get(src) + ", ";
		}
		long end_time = System.currentTimeMillis();;//计时结束
		
		String[] file_name = testfile.split("\\\\");
		
		debug = new String[9];
		
		int debug_index = 0;
		
		debug[debug_index++] = "\n\r" + file_name[file_name.length-1];
			
		debug[debug_index++] = "源节点为 ： " + str;
		
		debug[debug_index++] = "源节点级别为 ： " + str_rank;
		
		debug[debug_index++] = "总需求 ：" + total_node_need + " 满足需求：" + total_flow_best;
				
		debug[debug_index++] = "总花费： " + result_cost_best;
				
		debug[debug_index++] = "MCMF计算" + MCMF_COUNT + "次	平均每次用时" +  (end_time - begin_time)/(float)MCMF_COUNT + "ms";
				
		debug[debug_index++] = "本次用时 ： " + (end_time - begin_time) + "ms";	
				
		System.out.println("\n\r" + file_name[file_name.length-1] );
		
		System.out.println("源节点为 ： " + str);
		
		System.out.println("源节点级别为 ： " + str_rank);
		
		System.out.println("总需求 ：" + total_node_need + " 满足需求：" + total_flow_best);
		
		System.out.print("总花费： " + result_cost_best);
		
		System.out.print("	MCMF计算" + MCMF_COUNT + "次:平均每次用时" +  (end_time - begin_time)/(float)MCMF_COUNT + "ms" );
		
		System.out.print("\n本次用时 ： " + (end_time - begin_time) + "ms" );	
		
		return writePath();	
	}
	
	/** 
	 * 点 和 给它提供流量的源点
	 * @return 导出digraph_map_best中的流，变成一行的路径，用于输出
	 */
	private ArrayList<HashMap<Integer,Integer>> tars_src() {
		 
		ArrayList<HashMap<Integer,Integer>> tars_src = new ArrayList<>();
		for ( int index = 0 ; index < node_num ; index++ ) {
			tars_src.add( new HashMap<Integer,Integer>() );
		}
		
		for ( int u = 0 ; u < digraph_map.size() ; u++ ) {  //复制最好图的边流量
			for ( int v : digraph_map.get(u).keySet() ) {
				digraph_map.get(u).get(v).flow = digraph_map_best.get(u).get(v).flow;
			}
		}
		
		for	( Edge edge : digraph_map.get(super_src).values() ) {
			while ( edge.flow != 0 ) {
				int delete = MY_MAX;
				Edge relay = edge;
				ArrayList<Edge> path = new ArrayList<>(); 
				while ( relay.next != super_tar ) {
					if ( delete > relay.flow ) {
						delete = relay.flow;
					}
					path.add(relay);
					Collection<Edge> edges2 = digraph_map.get(relay.next).values();
					for ( Edge edge2 : edges2 ) {
						if ( edge2.flow != 0 ) {
							relay = edge2;
							break;
						}
					}
				}
				path.add(relay);        //流向超级汇点的边也要记录
				if ( delete > relay.flow ) {
					delete = relay.flow;
				}
				Edge to_adjust = null;
				int src_node = path.get(0).next;
				path.get(0).flow -= delete;
				for ( int index  = 1 ; index < path.size() ; index++ ) {
					to_adjust = path.get(index);
					to_adjust.flow -= delete;
					if ( index > 2 && to_adjust.next != super_tar ) {
						if ( tars_src.get(to_adjust.next).containsKey(src_node) ) {
							int temp_flow = tars_src.get(to_adjust.next).get(src_node);
							tars_src.get(to_adjust.next).put( src_node, temp_flow + delete);
						} else {
							tars_src.get(to_adjust.next).put( src_node, delete);
						}
					} 
				}
			}
		}
		return tars_src;
	}
	
	/**
	 * 交换操作
	 * @param node
	 * @return
	 */
	private boolean tryExchange( int node ) {
//		System.out.print("	||	Exchange");
		boolean success = false;
		/*******************************************************/
		/************************ 交换操作  ************************/	
		if ( need_find_src ) {
			need_find_src = false;
			tar_srcs = tars_src();
		}
		HashMap<Integer,Integer> node_give_flow = tar_srcs.get(node);
		int exchange_count = 0;
		while ( exchange_count < EX_FACT && !node_give_flow.isEmpty() ) {
			exchange_count++;
			int ex_server = -1;
			int bigest_give_flow = 0;
			for ( int server_num : node_give_flow.keySet() ) {
				if ( bigest_give_flow < node_give_flow.get(server_num) ) {
					bigest_give_flow = node_give_flow.get(server_num);
					ex_server = server_num;
				}
			}
			node_give_flow.remove(ex_server);	
			int temp_rank = srcs_now.get(ex_server);
			buildEdge( digraph_map, node, ex_server, temp_rank);    //添加边
			srcs_now.remove(ex_server);           //更新源（添加）
			srcs_now.put( node, temp_rank);
			networkSimplex();
			if ( total_flow_now == total_flow_best && result_cost_best > result_cost_now ) { //添加成功
//				System.out.print(" Success!!!");
				buildEdge( digraph_map_best, node, ex_server, temp_rank );  //同步图结构
				update_best();
				is_server[node] = true;
				is_server[ex_server] = false;
				success = true;
				break;
			} else {         //失败，恢复图
//				System.out.print(" Failed!");
				buildEdge( digraph_map, ex_server, node, temp_rank);    //添加边
				srcs_now.remove(node);           //更新源（添加）
				srcs_now.put( ex_server, temp_rank);
			}
		}
		return success;
	}
	
	private int getBestRank( int node ) {
		int best_cost = MY_MAX;
		int best_rank = -1;
		for ( int rank = 0; rank <= biggest_rank ; rank++ ) {
			int temp_cost = ( server_ability_cost[rank][1] + deploy_cost[node] ) /  server_ability_cost[rank][0];
			if ( temp_cost < best_cost ) { //小于，还是小于等于
				best_rank = rank;
				best_cost = temp_cost;
			}
		}
		return best_rank;
	}

	private boolean tryDownRank( int node, boolean succession ) {
//		System.out.print("	||	Down_rank");
		boolean success = false;
		int temp_rank = srcs_now.get(node);
		boolean is_success = true;
		while ( is_success && temp_rank > 0 ) {
			temp_rank--;
			srcs_now.put( node, temp_rank);
			digraph_map.get(super_src).get(node).cap = server_ability_cost[temp_rank][0];
			networkSimplex(); 
			if ( total_flow_now == total_flow_best && result_cost_best > result_cost_now ) { //降级成功
//				System.out.print(" Success!!!");
				digraph_map_best.get(super_src).get(node).cap = server_ability_cost[temp_rank][0];
				update_best();
				success = true;
				is_success = true;
			} else {
//				System.out.print(" Failed!");
				is_success = false;
				srcs_now.put( node, temp_rank + 1 );
				digraph_map.get(super_src).get(node).cap = server_ability_cost[temp_rank + 1][0];
			}
			if ( !succession ) {
				break;
			}
		}
		return success;
	}
	
	private int tryMove( int node, boolean change_rank ) {
//		System.out.print("	||	Move");
		Collection<Edge> edges = digraph_map_best.get(node).values();
		HashMap<Integer,Integer> neighbor_no_server = new HashMap<>();
		for ( Edge edge : edges ) {
			if ( !is_server[edge.next] && edge.next != super_src && edge.next != super_tar && edge.flow != 0 ) {
				neighbor_no_server.put(edge.next, deploy_cost[edge.next]);
			}
		}
		int move_count = 0;
		int move_node = -1;
		boolean success = false;
		while ( !neighbor_no_server.isEmpty() && move_count < MOVE_FACT ) {
			int min_cost = MY_MAX;
			int this_cost = 0;
			for ( int nns : neighbor_no_server.keySet() ) {
				this_cost = neighbor_no_server.get(nns);
				if ( this_cost < min_cost ) {
					min_cost = this_cost;
					move_node = nns;
				}
			}
			move_count++;
			neighbor_no_server.remove(move_node);
			int temp_rank = srcs_now.get(node);
			int move_rank = -1;
			if ( change_rank ) {
				move_rank = getBestRank(move_node);
			} else {
				move_rank = temp_rank;
			}
			
			buildEdge( digraph_map, move_node, -1, move_rank);   //添加邻居服务器
			buildEdge( digraph_map, -1, node, -1);   //删除自身服务器 
			srcs_now.remove(node);
			srcs_now.put( move_node, move_rank);
			networkSimplex();  
			if ( total_flow_now == total_flow_best && result_cost_best > result_cost_now ) {
//				System.out.print(" Success!!!");
				buildEdge( digraph_map_best, move_node, -1, move_rank );    //同步图
				buildEdge( digraph_map_best, -1, node, -1 );    //同步图
				update_best();
				is_server[move_node] = true;
				is_server[node] = false;     
				success = true;
				break;
			} else {
//				System.out.print(" Failed");
				buildEdge( digraph_map, -1, move_node, -1 );      //恢复边
				buildEdge( digraph_map, node, -1, temp_rank );  
				srcs_now.remove(move_node);
				srcs_now.put( node, temp_rank);
			}
		}
		return success ? move_node : -1;
	}
	
	
	
	private boolean tryDelete( int node ){
//		System.out.print("	||	Delete");
		boolean success = false;
		if ( digraph_map_best.get(super_src).get(node).flow == 0 ) {  //直接就可以删除了
			buildEdge( digraph_map, -1, node, -1 );    //删除边
			buildEdge( digraph_map_best, -1, node, -1 );    //删除边
			result_cost_best -= deploy_cost[node];
			result_cost_best -= server_ability_cost[srcs_now.get(node)][1];
			srcs_now.remove(node);
			is_server[node] = false;
			success = true;
//			System.out.print(" Success!!!");
		} else {                                   //强行删除
			buildEdge( digraph_map, -1, node, -1 );    //删除边
			int temp_rank = srcs_now.get(node);
			srcs_now.remove( node );   
			networkSimplex(); 
			if ( total_flow_now == total_flow_best && result_cost_best > result_cost_now ) {
//				System.out.print(" Success!!!");
				buildEdge( digraph_map_best, -1, node, -1 );   //同步图结构
				update_best(); 
				is_server[node] = false;
				success = true;
			} else {
//				System.out.print(" Failed!");
				buildEdge( digraph_map, node, -1, temp_rank );	//恢复图
				srcs_now.put( node, temp_rank ); 
			}
		}
		return success;
	}
	
	
	
	/**
	 * 更新digraph_map_best为digraph_map,事实上就是将他们交换，同时将别的相关信息也更新了（最好费用，最大流量）
	 */
	private void update_best() {	
		ArrayList<HashMap<Integer,Edge>> digraph_map_exchange;
		digraph_map_exchange = digraph_map_best;   //初始化最优值
		digraph_map_best = digraph_map;
		digraph_map = digraph_map_exchange;
		result_cost_best = result_cost_now;
		total_flow_best = total_flow_now;
	}

	/**
	 * 网络单纯形
	 */
	private void networkSimplex() {
		MCMF_COUNT++;
		result_cost_now = PRICE_MAX * total_node_need * 2;
		total_flow_now = 0;
		int ffp_flag = 0;
		all_edges.clear();
		for ( int u = 0 ; u < digraph_map.size() ; u++ ) {  //初始化赋权有向图 所有边流量 
			Collection<Edge> uv = digraph_map.get(u).values();
			for ( Edge edge : uv ) {
				edge.flow = 0;
				edge.is_in_tree = false;
				edge.for_find_path = ffp_flag;
				all_edges.add(edge);
			}
		}
		T0_edge[super_src].flow = total_node_need;
		T0_edge[super_src].is_in_tree = true;
		T0_edge[super_src].tree_pre_edge = null;
		T0_edge[super_src].Tree_next_head = T0_edge[0];
		T0_edge[super_src].brother_pre = null;
		T0_edge[super_src].brother_next = null;
		T0_edge[super_src].for_find_path = ffp_flag;
		T0_edge[super_src].is_forward = true;
		all_edges.add(T0_edge[super_src]);
		
		for ( int node = 1 ; node < node_num - 1 ; node++ ) {  //人工点到图中每一个点（不包括超级源和汇）
			T0_edge[node].flow = 0;
			T0_edge[node].is_in_tree = true;
			T0_edge[node].tree_pre_edge = T0_edge[super_src];
			T0_edge[node].Tree_next_head = null;
			T0_edge[node].brother_pre = T0_edge[ node - 1 ];
			T0_edge[node].brother_next = T0_edge[ node + 1 ];
			T0_edge[node].for_find_path = ffp_flag;
			T0_edge[node].is_forward = true;	
			all_edges.add(T0_edge[node]);
		}
		
		T0_edge[0].flow = 0;
		T0_edge[0].is_in_tree = true;
		T0_edge[0].tree_pre_edge = T0_edge[super_src];
		T0_edge[0].Tree_next_head = null;
		T0_edge[0].brother_pre = null;
		T0_edge[0].brother_next = T0_edge[1];
		T0_edge[0].for_find_path = ffp_flag;
		T0_edge[0].is_forward = true;	
		all_edges.add(T0_edge[node_num-1]);
		
		T0_edge[node_num-1].flow = 0;
		T0_edge[node_num-1].is_in_tree = true;
		T0_edge[node_num-1].tree_pre_edge = T0_edge[super_src];
		T0_edge[node_num-1].Tree_next_head = null;
		T0_edge[node_num-1].brother_pre = T0_edge[node_num-2];
		T0_edge[node_num-1].brother_next = T0_edge[super_tar];
		T0_edge[node_num-1].for_find_path = ffp_flag;
		T0_edge[node_num-1].is_forward = true;	
		all_edges.add(T0_edge[node_num-1]);
		
		T0_edge[super_tar].flow = total_node_need;
		T0_edge[super_tar].is_in_tree = true;
		T0_edge[super_tar].tree_pre_edge = T0_edge[super_src];
		T0_edge[super_tar].Tree_next_head = null;
		T0_edge[super_tar].brother_pre = T0_edge[node_num-1];
		T0_edge[super_tar].brother_next = null;
		T0_edge[super_tar].for_find_path = ffp_flag;
		T0_edge[super_tar].is_forward = true;	
		all_edges.add(T0_edge[super_tar]);
		int edge_index = 0;
		boolean flag = true;
		pai[super_src] = 0;
		node_pre_edge[super_src] = null;
		updatePai( super_src, T0_edge[super_src]);            //首次更新所有节点的势
		while ( true ) {     //大循环	
			Edge in_edge = null;
			while ( edge_index < all_edges.size() ) {
				Edge edge_temp = all_edges.get(edge_index);
				if ( !edge_temp.is_in_tree ) {
					int critical_number = edge_temp.price - pai[edge_temp.pre] + pai[edge_temp.next];
					if ( edge_temp.flow == 0 ) {
						if ( critical_number < 0 ) {
							in_edge = edge_temp;
							break;
						}
					} else {
						if ( critical_number > 0 ) {
							in_edge = edge_temp;
							break;
						}
					}
				}
				edge_index++;
			}
			if ( in_edge == null ) {   //已经找到最优解
				if ( !flag ) {
					edge_index = 0;
					flag = true;
				} else {
					break;
				}
			} else {
				flag = false;
				ffp_flag = (ffp_flag + 2)%MY_MAX;
				boolean in_edge_direction = true;
				Edge left_edge, right_edge;
				if ( in_edge.flow == 0 ) {
					left_edge = node_pre_edge[in_edge.next];
					right_edge = node_pre_edge[in_edge.pre];
				} else {
					in_edge_direction = false;
					left_edge = node_pre_edge[in_edge.pre];
					right_edge = node_pre_edge[in_edge.next];
				}
				
				Edge relay_edge = left_edge;
				while ( relay_edge != null ) {
					relay_edge.for_find_path = ffp_flag;
					relay_edge = relay_edge.tree_pre_edge;
				}
				int min = MY_MAX;
				
				boolean candidate_side = false;
				Edge candidate = null;
				relay_edge = right_edge;
				while ( relay_edge != null && relay_edge.for_find_path != ffp_flag ) {
					int can_adjust;
					if ( relay_edge.is_forward ) {
						can_adjust = relay_edge.cap - relay_edge.flow;
					} else {
						can_adjust = relay_edge.flow;
					}
					if ( can_adjust < min ) {
						min = can_adjust;
						candidate = relay_edge;
					}
					relay_edge = relay_edge.tree_pre_edge;
				}
				Edge cross_edge = relay_edge;
				relay_edge = left_edge;
				while ( relay_edge != cross_edge ) {
					int can_adjust;
					if ( relay_edge.is_forward ) {
						can_adjust = relay_edge.flow;
					} else {
						can_adjust = relay_edge.cap - relay_edge.flow;
					}
					if ( can_adjust <= min ) {
						min = can_adjust;
						candidate = relay_edge;
						candidate_side = true;
					}
					relay_edge = relay_edge.tree_pre_edge;
				}
				if ( in_edge.cap <= min ) {
					candidate = null;
					min = in_edge.cap;
				}
				relay_edge = left_edge;
				while ( relay_edge != cross_edge ) {
					if ( relay_edge.is_forward ) {
						relay_edge.flow -= min;
						result_cost_now -= min * relay_edge.price;
					} else {
						relay_edge.flow += min;
						result_cost_now += min * relay_edge.price;
					}
					relay_edge = relay_edge.tree_pre_edge;
				}
				relay_edge = right_edge;
				while ( relay_edge != cross_edge ) {
					if ( relay_edge.is_forward ) {
						relay_edge.flow += min;
						result_cost_now += min * relay_edge.price;
					} else {
						relay_edge.flow -= min;
						result_cost_now -= min * relay_edge.price;
					}
					relay_edge = relay_edge.tree_pre_edge;
				}
				if ( in_edge.flow == 0 ) {
					in_edge.flow += min;
					result_cost_now += min * in_edge.price;
				} else {
					in_edge.flow -= min;
					result_cost_now -= min * in_edge.price;
				}
				if ( candidate != null ) {
					candidate.is_in_tree = false;
					deleteEdgeInFather(candidate);
					Edge break_side_edge, other_side_edge;
					in_edge.is_in_tree = true;
					if ( candidate_side ) { 
						break_side_edge = left_edge;
						other_side_edge = right_edge;
						in_edge.is_forward = in_edge_direction;
					} else {
						break_side_edge = right_edge;
						other_side_edge = left_edge;
						in_edge.is_forward = !in_edge_direction;
					}
					
					relay_edge = in_edge;
					Edge pre_edge = break_side_edge;	
					while ( pre_edge != candidate ) {
						deleteEdgeInFather(pre_edge);
						if ( pre_edge.Tree_next_head != null ) {			
							Edge edge_temp = pre_edge.Tree_next_head;
							while ( edge_temp != null ) {
								edge_temp.tree_pre_edge = relay_edge;
								edge_temp = edge_temp.brother_next;
							}
							pre_edge.brother_next = pre_edge.Tree_next_head;
							pre_edge.brother_pre = null;
							pre_edge.Tree_next_head.brother_pre = pre_edge;
							pre_edge.Tree_next_head = null;
							relay_edge.Tree_next_head = pre_edge;
						} else {
							relay_edge.Tree_next_head = pre_edge;
							pre_edge.brother_next = null;
							pre_edge.brother_pre = null;
						}
						Edge pre_pre_edge = pre_edge.tree_pre_edge;
						pre_edge.tree_pre_edge = relay_edge;
						relay_edge = pre_edge;
						pre_edge = pre_pre_edge;
						relay_edge.is_forward = !relay_edge.is_forward;
					}
					
					if ( candidate.Tree_next_head != null ) {			
						Edge edge_temp = candidate.Tree_next_head;
						while ( edge_temp != null ) {
							edge_temp.tree_pre_edge = relay_edge;
							edge_temp = edge_temp.brother_next;
						}
					} 
					relay_edge.Tree_next_head = pre_edge.Tree_next_head;
					
					in_edge.tree_pre_edge = other_side_edge;
					if ( other_side_edge != null ) {
						if ( other_side_edge.Tree_next_head != null ) {
							other_side_edge.Tree_next_head.brother_pre = in_edge;
							in_edge.brother_pre = null;
							in_edge.brother_next = other_side_edge.Tree_next_head;
							other_side_edge.Tree_next_head = in_edge;
						} else {
							other_side_edge.Tree_next_head = in_edge;
							in_edge.brother_pre = null;
							in_edge.brother_next = null;
						}
					}
					if ( in_edge.is_forward ) {
						updatePai( in_edge.pre, in_edge);
					} else {
						updatePai( in_edge.next, in_edge);
					}
				}
				edge_index++;
			}
		}
		
		for ( Edge edge : T0_edge ) {
			if ( edge.flow != 0 ) {
				result_cost_now -= edge.flow * edge.price; 
			}
		}
		
			for ( Edge edge : digraph_map.get(super_src).values() ) {
				total_flow_now += edge.flow;
			}
			
			for ( int node : srcs_now.keySet() ) {
				result_cost_now += deploy_cost[node];
				result_cost_now += server_ability_cost[ srcs_now.get(node) ][1];
			}

	}
	
	private void deleteEdgeInFather ( Edge delete ) {
		if ( delete.tree_pre_edge != null ) {
			if ( delete.brother_pre != null ) {
				if ( delete.brother_next != null ) {
					delete.brother_pre.brother_next = delete.brother_next;
					delete.brother_next.brother_pre = delete.brother_pre;
				} else {
					delete.brother_pre.brother_next = null;
				}
			} else {
				if ( delete.brother_next != null ) {
					delete.tree_pre_edge.Tree_next_head = delete.brother_next;
					delete.brother_next.brother_pre = null;
				} else {
					delete.tree_pre_edge.Tree_next_head = null;
				}
			}
		}
		delete.brother_pre = null;
		delete.brother_next = null;
	}
	
	private void updatePai( int begin_node, Edge edge) {
		Edge edge_next;
		if ( edge.pre == begin_node ) {
			node_pre_edge[edge.next] = edge;
			pai[edge.next] = pai[edge.pre] - edge.price;
			edge_next = edge.Tree_next_head;
			while ( edge_next != null ) {
				updatePai( edge.next, edge_next );
				edge_next = edge_next.brother_next;
			}
		} else {
			node_pre_edge[edge.pre] = edge;
			pai[edge.pre] = pai[edge.next] + edge.price;
			edge_next = edge.Tree_next_head;
			while ( edge_next != null ) {
				updatePai( edge.pre, edge_next );
				edge_next = edge_next.brother_next;
			}
		}
	}
	
	
	/**
	 * 算法初始化，读取数据，初始化各种全局变量
	 * @return 成功为true（实际没用）
	 */
	private boolean algorithmInit( String[] data ) {
	
		String[] inline = data[0].split(" ");
		node_num = Integer.parseInt(inline[0]);
		edge_num = Integer.parseInt(inline[1]);
		tar_node_num = Integer.parseInt(inline[2]);
		
		int index = 2; 
		while ( !data[index].equals("") ) {	       //级别
			inline = data[index].split(" ");
			biggest_rank++;
			server_ability_cost[biggest_rank][0] = Integer.parseInt(inline[1]);
			server_ability_cost[biggest_rank][1] = Integer.parseInt(inline[2]);
			index++;
		}
		
		deploy_cost = new int[node_num];
		
		pai = new int[ node_num + 3 ];  //存储势
		
		is_server = new boolean[ node_num + 3 ];
		
		is_tar = new boolean[node_num + 3];
		
		node_pre_edge = new Edge[node_num + 3];
		
		T0_edge = new Edge[node_num + 2];
		
		index++;
		int deploy_cost_index = 0;
		while ( deploy_cost_index < node_num ) {    //每个点费用
			inline = data[index].split(" ");
			deploy_cost[deploy_cost_index] = Integer.parseInt(inline[1]);
			index++;
			deploy_cost_index++;
		}
		
		/********************** 第一，初始化有向图 ************************/
		for ( int num = 0 ; num < node_num + 2 ; num++ ) {  //初始化 有向图 new 对象
			HashMap<Integer,Edge> init1 = new HashMap<>();
			HashMap<Integer,Edge> init2 = new HashMap<>();
			digraph_map.add(init1);
			digraph_map_best.add(init2);
			
			is_tar[num] = false;
		}
		
		super_src = node_num;  //记下超级源点超级汇点的位置
		super_tar = super_src + 1;
		
		int us, vs;     // 边  u -> v
		index++;
		for ( int edge_index = index ; edge_index < edge_num + index ; edge_index++ ) {  //存入边，同时存入两个图中	
			int edge_capacity = 0;
			int edge_cost = 0;
			inline = data[edge_index].split(" ");
			
			us = Integer.parseInt(inline[0]);
			vs = Integer.parseInt(inline[1]);
			edge_capacity = Integer.parseInt(inline[2]);
			edge_cost = Integer.parseInt(inline[3]);					
			
			Edge u_v = new Edge( us, vs, edge_capacity, edge_cost, 0 );
			Edge v_u = new Edge( vs, us, edge_capacity, edge_cost, 0 );
			
			digraph_map.get(us).put( vs, u_v);
			digraph_map.get(vs).put( us, v_u);
			
			Edge u_v1 = new Edge( us, vs, edge_capacity, edge_cost, 0 );
			Edge v_u1 = new Edge( vs, us, edge_capacity, edge_cost, 0 );
			
			digraph_map_best.get(us).put( vs, u_v1);
			digraph_map_best.get(vs).put( us, v_u1);
					
		}
		
		int real_tar , tar_node , need;    //需求点需求量初始化
		index++;
		for ( int tar_index = edge_num + index ; tar_index < edge_num + index + tar_node_num ; tar_index++ ) {
			inline = data[tar_index].split(" ");
			real_tar = Integer.parseInt(inline[0]);
			tar_node = Integer.parseInt(inline[1]);
			need = Integer.parseInt(inline[2]);
			
			tars_need.put( tar_node, need);
			tars_real_number.put( tar_node, real_tar);
			total_node_need += need;
		}
		
		//这里更改，固定超级汇点！！！ 超级汇点连接所有需求节点！！！ 不需要改动！！！
		for ( int tar : tars_need.keySet() ) {		     
			Edge tar_suptar = new Edge( tar, super_tar, tars_need.get(tar), 0, 0 );
			digraph_map.get(tar).put( super_tar, tar_suptar);
			
			Edge tar_suptar1 = new Edge( tar, super_tar, tars_need.get(tar), 0, 0 );
			digraph_map_best.get(tar).put( super_tar, tar_suptar1);
			
			is_tar[tar] = true;
		}
		
		/************************ 单纯形的初始化 T0_edge, 此集合的边不会变动  **************************/
		
		d0 = digraph_map.size();   //人工点
		Edge d0_v;
		for ( int node = 0 ; node < node_num ; node++ ) {  //人工点到图中每一个点（不包括超级源和汇）
			d0_v = new Edge( d0, node, MY_MAX, PRICE_MAX, 0 );  //容量无穷，花费无穷
			T0_edge[node] = d0_v;
		}
		d0_v = new Edge( super_src, d0, MY_MAX, PRICE_MAX, total_node_need );  //容量无穷，花费无穷，流量为需求
		T0_edge[super_src] = d0_v;
		d0_v = new Edge( d0, super_tar, MY_MAX, PRICE_MAX, total_node_need );  //容量无穷，花费无穷，流量为需求
		T0_edge[super_tar] = d0_v;	
			
		return true;
	}
	
	/**
	 * 用于添加或者删除源时，更改图上的路径，一次只能加一个点或者删除一个点
	 * @param the_map 需要更改路径的图
	 * @param new_srcs 新的源
	 * @param delete_srcs 需要删除的源
	 */
	private void buildEdge(ArrayList<HashMap<Integer, Edge>> the_map, int new_srcs, int delete_srcs, int rank) {
		if ( delete_srcs != -1 ) {  //删除老的服务器点 以及它的边
			the_map.get(super_src).remove(delete_srcs);
		}
		
		if ( new_srcs != -1 ) {   //添加新的服务器点 需要加入边  容量安照rank设置
			Edge supsrc_ns = new Edge( super_src, new_srcs, MY_MAX, 0, 0 );
			supsrc_ns.cap = server_ability_cost[rank][0];
			the_map.get(super_src).put( new_srcs, supsrc_ns);
		}
		
	}
	
	/** 
	 * 写出最后的结果到文件
	 * @return 导出digraph_map_best中的流，变成一行的路径，用于输出
	 */
	private String[] writePath() {
		
		LinkedList<String> the_path_str = new LinkedList<>(); 
		int flow_test = 0;
		int cost_test = 0;
		for	( Edge edge : digraph_map_best.get(super_src).values() ) {
			while ( edge.flow != 0 ) {
				String str = "";
				int delete = MY_MAX;
				Edge relay = edge;
				LinkedList<Edge> path = new LinkedList<>(); 
				while ( relay.next != super_tar ) {
					if ( delete > relay.flow ) {
						delete = relay.flow;
					}
					path.add(relay);
					Collection<Edge> edges2 = digraph_map_best.get(relay.next).values();
					for ( Edge edge2 : edges2 ) {
						if ( edge2.flow != 0 ) {
							relay = edge2;
							break;
						}
					}
				}
				path.add(relay);        //流向超级汇点的边也要记录
				if ( delete > relay.flow ) {
					delete = relay.flow;
				}
				flow_test += delete;
				for ( Edge to_adjust : path ) {
					to_adjust.flow -= delete;
					cost_test += delete * to_adjust.price;
					if ( to_adjust.next != super_tar ) {
						str += to_adjust.next + " ";
					} else {
						str += tars_real_number.get(to_adjust.pre) + " " + delete;
					}
				}
				str += " " + srcs_now.get(edge.next);
				the_path_str.add(str);
			}
		}
		
		String[] arr_to_write = new String[the_path_str.size() + 2];
		arr_to_write[0] = "" + the_path_str.size();
		arr_to_write[1] = "";
		for ( int tw = 0 ; tw < the_path_str.size() ; tw++ ) {
			arr_to_write[ tw + 2 ] = the_path_str.get(tw);
		}
		System.out.print("	纯流量费用: " + cost_test);
		for ( int node : srcs_now.keySet() ) {
			cost_test += deploy_cost[node];
			cost_test += server_ability_cost[srcs_now.get(node)][1];	
		}
		debug[7] = "费用检测：" + cost_test + "   流量检测：" + flow_test;
		
		debug[8] = " ";
		
		System.out.println("\n费用检测：" + cost_test + "   流量检测：" + flow_test);
		
		return arr_to_write;
	}
}
