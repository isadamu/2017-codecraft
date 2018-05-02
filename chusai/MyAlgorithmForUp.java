package new6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeMap;

public class MyAlgorithmForUp {    //3-28 中午上传
	
	public String test_str = "";
	
	private int MCMF_COUNT = 0;
	
	private int all_node_need = 0;
	
	private int move_fact = 2;              //源节点移动次尝试移动几个方向
	
	public static final int MY_MAX = 987654321;       //表示正无穷
	
	private ArrayList<HashMap<Integer,Edge>> digraph_map = new ArrayList<>();  //存储有向图 ，用于计算
	
	private ArrayList<HashMap<Integer,Edge>> digraph_map_best = new ArrayList<>();  //存储最好的流结果，和上面的图结构一致
	
	private int node_num = 0;               //节点数（不包含超级源点超级汇点）
	
	private int edge_num = 0;               //总的边数
	
	private int tar_node_num = 0;           //tar表示目标，需求点个数
	
	private int per_server_cost = 0;        //单个服务器费用
	
	private long begin_time = 0;            //记录开始时间
	
	private int time_limit = 88000;       //用时上限
	
	private HashMap<Integer,Integer> tars_need = new HashMap<>();  //需求点以及它的需求
	
	private HashMap<Integer,Integer> tars_real_number = new HashMap<>(); //需求点在图上的位置 与它实际的ID
	
	private LinkedList<Integer> srcs_best = new LinkedList<>();    //当前布置的源点
	
	private LinkedList<Integer> srcs_now = new LinkedList<>(); //调整布置的源点
	
	private int super_src = -1;                             //超级源点
	
	private int super_tar = -1;                             //超级汇点
	
	private int result_cost_best = MY_MAX;                    //存储最低的花费
	
	private int result_cost_now = MY_MAX;               //存储 临时花费
	
	private int total_flow_best = 0;                             //存储 最大流量
	
	private int total_flow_now = 0;                             //存储 临时流量
	
	private SpfaStruct[] node_preAndcost;                  //SPFA前向节点以及花费
	
	private LinkedList<Integer> spfa_queue = new LinkedList<>();   //SPFA需要更新的队列
	
	private boolean[] queue_not_contains = null;
	
	private int path_second_node = -1;                      //SPFA最短路连接超级源的节点
	
	
	/**
	 * 算法入口
	 * 
	 * 1，所有需求点部署一个服务器
	 * 2，尝试删除服务器，移动服务器，看能否更便宜
	 * 4，返回，结束
	 */
	public String[] algorithmRun(String[] data) {
		begin_time = System.currentTimeMillis();//开始计时
		algorithmInit(data);	
		/************************* 1，所有需求点部署一个服务 ***********************/
		LinkedHashMap<Integer,Integer> tars_server = new LinkedHashMap<>();     //队列，以及它关联的服务器位置
		LinkedList<Integer> tars_queue = new LinkedList<>();
		
		Integer[] to_sort = new Integer[tars_need.size()];
		to_sort = tars_need.values().toArray(to_sort);
		HashMap<Integer,Integer> tars_need_copy = new HashMap<>();
		tars_need_copy.putAll( tars_need);
		Arrays.sort(to_sort);
		for ( int ts_index = 0 ; ts_index < to_sort.length ; ts_index++ ) {    //建立初始边
			for ( int tar : tars_need_copy.keySet() ) {
				if ( tars_need_copy.get(tar) == to_sort[ts_index] ) {
					tars_need_copy.remove(tar);
					srcs_now.add(tar);
					buildEdge( digraph_map, tar, -1 );
					buildEdge( digraph_map_best, tar, -1 );
					tars_server.put( tar, tar);
					tars_queue.add( tar );
					break;
				}
			}
		}
		compute_MCMF();  //计算最小费用最大流
		update_best();  //首次更新最优值
		/*****************************************************************/
		/*2，取队列第一个元素，若它没有关联服务器，尝试部署一台 ， 否则 尝试删除 或者 移动，然后放到队尾*/
		int count = 0;
		long time_count = 0;
		TreeMap<Integer,Integer> flow_next = new TreeMap<>();
		while ( count < tar_node_num ) {
			int node = tars_queue.get(0);
			System.out.print("\n" + node);
			time_count = System.currentTimeMillis();
			if ( time_count - begin_time >  time_limit ) {
				break;
			}
			if ( tars_server.containsKey(node) ) {  //关联了服务器
				int server_position = tars_server.get(node);
				//尝试删除服务器	
				buildEdge( digraph_map, -1, server_position);    //删除边
				update_srcs_now( -1, server_position);           //更新源(删除)
				compute_MCMF();  
				if ( total_flow_now == total_flow_best && result_cost_best > result_cost_now ) { //删除成功
					System.out.print("	Delete Success!!!");
					buildEdge( digraph_map_best, -1, server_position);   //同步图结构
					update_best();
					count = 0;                                    //计算清零
					tars_server.remove(node);
				} else { //失败，恢复图，尝试移动     ** 移动成功应该继续尝试移动*****
					buildEdge( digraph_map, server_position, -1);	//恢复图
					int move_position, temp_node;
					temp_node = node;
					boolean move_success = true ;
					ArrayList<Integer> uesd = new ArrayList<>();
					uesd.add(server_position);
					while ( move_success ) {
						move_success = false;
						flow_next.clear();
						for ( Edge edge : digraph_map_best.get(server_position).values() ) {
							if ( !uesd.contains(edge.next) && edge.flow != 0 ) {
								flow_next.put( edge.flow, edge.next);
							}
						}
						int move_count = 0;
						while ( move_count < move_fact && !flow_next.isEmpty() ) { //选择最大边移动，若降低费用，则更新
							move_position = flow_next.get( flow_next.lastKey() );
							if (  move_position != super_tar && !srcs_best.contains(move_position) ) {
								System.out.print("	Moving ");
								uesd.add(move_position);
								buildEdge( digraph_map, move_position, server_position);    //删除边
								update_srcs_now( move_position, server_position);           //更新源（移动）
								compute_MCMF();  										//尝试移动服务器
								if ( total_flow_now == total_flow_best && result_cost_best > result_cost_now ) { //移动成功
									System.out.print(" Success!!!");
									buildEdge( digraph_map_best, move_position, server_position);
									update_best();
									count = 0;
									if ( tars_queue.contains( move_position) ) {  //移动到了另一个源点
										tars_server.remove(temp_node);
										tars_server.put( move_position, move_position );
										temp_node = move_position;
									} else {
										tars_server.put( temp_node, move_position);
									}
									
									move_success = true ;
									server_position = move_position;
									break;	
								} else { //失败，恢复图
									System.out.print(" Failed!!!");
									buildEdge( digraph_map, server_position, move_position);
								}
								move_count++;
							}
							flow_next.remove(flow_next.lastKey());
						}
					}
					
				}
			} else {
				//没有关联服务器        //尝试添加服务器
				buildEdge( digraph_map, node, -1);    //添加边
				update_srcs_now( node, -1);           //更新源（添加）
				compute_MCMF();  
				if ( total_flow_now == total_flow_best && result_cost_best > result_cost_now ) { //添加成功
					System.out.print(" Add Success!!!");
					buildEdge( digraph_map_best, node, -1);  //同步图结构
					update_best();
					count = 0;
					tars_server.put( node, node);	
				} else {         //失败，恢复图
					buildEdge( digraph_map, -1, node);
				}
			}
			tars_queue.remove(0);
			tars_queue.add(node);	
			count++;
		}	
		
		/***************** 返回输出结果 ***********************/
		String str = "";
		for ( int src : srcs_best ) {
			str += src + ", ";
		}
		
		String[] file_name = test_str.split("\\\\");
		
		System.out.println("\n\r" + file_name[file_name.length-1]);
		
		System.out.println("源节点为 ： " + str);
		
		System.out.println("总需求 ：" + all_node_need + " 满足需求：" + total_flow_best);
		
		System.out.print("总花费： " + result_cost_best);
		
		System.out.print("	MCMF计算" + MCMF_COUNT + "次");
		
//		FileUtil.write("F:\\我的坚果云\\hwrj\\hwrj\\result_new.txt", path_result, false);		
	
		long end_time = System.currentTimeMillis();;//计时结束
		System.out.print("	本次用时 ： " + (end_time - begin_time) + "ms" );
		
		return writePath();
	}
	
	
	/**
	 * 计算最小费用最大流 （核心）
	 */
	private void compute_MCMF() {
		MCMF_COUNT++;
		/************************** 图的初始化 ，标记的初始化 *************************/
		result_cost_now = 0;
		total_flow_now = 0;
		for ( int u = 0 ; u < digraph_map.size() ; u++ ) {  //初始化赋权有向图 所有边权值 
			Collection<Edge> uv = digraph_map.get(u).values();
			for ( Edge edge : uv ) {
				edge.reInit();
			}
		}	
		for ( int i = 0 ; i < node_preAndcost.length ; i++ ) {  //SPFA标记初始化
			node_preAndcost[i].reInit();
			queue_not_contains[i] = true;
		}
		node_preAndcost[super_src].set( null, 0, -1);
		spfa_queue.add(super_src);               //将源点加入更新队列
		queue_not_contains[super_src] = false;
		
		/************************** SPFA开始计算  *************************/
		int spfa_count = 0;
		while( SPFA() ) {	//直到找不到最短路
			spfa_count++;
			spfa_queue.add(super_src);   
			queue_not_contains[super_src] = false;
			for ( int i = 0 ;  i < node_preAndcost.length ; i++ ) {//所有点加入更新队列，但是并不是所有点的参数都需要设置为0  
				if ( node_preAndcost[i].begin_node == path_second_node ) {		
					node_preAndcost[i].reInit();
					for ( int v : digraph_map.get(i).keySet() ) {
						if ( queue_not_contains[v] ) {
							spfa_queue.add(v);
							queue_not_contains[v] = false;
						}
					}
				}
			}	
		}	
		System.out.println("  spfa:" + spfa_count);
		/**************************** 最小费用最大流计算完毕 输出结果 ***********************/
		result_cost_now += per_server_cost * srcs_now.size();		
	}
	
	
	/**
	 * SPFA寻找最短路，实际上兼顾对图中流的修改
	 * @return 找到最短路返回true，否则false
	 */
	private boolean SPFA() {
		/********************** 第一，寻找最短路 ************************/
		int u;
		while ( !spfa_queue.isEmpty() ) {
			u = spfa_queue.get(0);
			Collection<Edge> Edges = digraph_map.get(u).values();
			spfa_queue.remove(0);
			queue_not_contains[u] = true;
			for ( Edge edge : Edges ) {
				if ( edge.use_1 ) {  //正向可更新
					if ( edge.spfa_1 + node_preAndcost[u].distance < node_preAndcost[edge.next].distance ) {
						node_preAndcost[edge.next].pre_edge = edge;
						node_preAndcost[edge.next].distance = node_preAndcost[u].distance + edge.spfa_1;
						if ( u == super_src ) {
							node_preAndcost[edge.next].begin_node = edge.next;
						} else {
							node_preAndcost[edge.next].begin_node = node_preAndcost[u].begin_node;
						}
						if ( queue_not_contains[edge.next] ) {
							spfa_queue.add(edge.next);
							queue_not_contains[edge.next] = false;
						}
					}
				} else if ( node_preAndcost[u].distance + edge.spfa_2 < node_preAndcost[edge.next].distance ) { //反向可更新
					node_preAndcost[edge.next].pre_edge = edge;
					node_preAndcost[edge.next].distance = node_preAndcost[u].distance + edge.spfa_2;
					if ( u == super_src ) {
						node_preAndcost[edge.next].begin_node = edge.next;
					} else {
						node_preAndcost[edge.next].begin_node = node_preAndcost[u].begin_node;
					}
					if ( queue_not_contains[edge.next] ) {
						spfa_queue.add(edge.next);
						queue_not_contains[edge.next] = false;
					}
				}
			}
		}
		/******************** 第二，安照当前找到的最短路调整流表 **************************/
		if ( node_preAndcost[super_tar].pre_edge == null ) {  //没找到最短路
			return false;
		} else {
			LinkedList<Edge> path = new LinkedList<>();
			int adjust = MY_MAX;                 	//调整量
			int pnode = super_tar;
			Edge path_edge = null;
			while ( pnode != super_src ) {       	//向源点回溯
				path_second_node = pnode;
				int pre = node_preAndcost[pnode].pre_edge.pre;
				path_edge = node_preAndcost[pnode].pre_edge;
				path.add(path_edge);
				pnode = pre;
				if ( path_edge.use_1 ) {       		//使用的正向边
					if ( adjust > path_edge.cap - path_edge.flow ) {
						adjust = path_edge.cap - path_edge.flow;
					}
				} else {                 			//反向边   需查找正向边中使用的流量
					if ( adjust > path_edge.edge_re.flow ) {
						adjust = path_edge.edge_re.flow;
					}
				}
			}
			total_flow_now += adjust;
			for ( Edge edge : path ) {
				if ( edge.use_1 ) {       			//使用的是正向边   
					edge.flow += adjust;
					result_cost_now += adjust * edge.price;
					edge.edge_re.use_1 = false;
					if ( edge.flow == edge.cap ) {   //流量等于容量     正向权重设为无穷
						edge.spfa_1 = MY_MAX;
					} else {
						edge.spfa_1 = edge.price;
					}
					edge.edge_re.spfa_2 = -1 * edge.price;
				} else {                       		//使用的是反向边  需要调整 正向边
					edge.edge_re.flow -= adjust;
					result_cost_now -= adjust * edge.edge_re.price;
					if ( edge.edge_re.flow == 0 ) {           //正向流量为0     反向权重设置为无穷
						edge.spfa_2 = MY_MAX;
						edge.use_1 = true;
					} else {
						edge.spfa_2 = -1 * edge.edge_re.price;
					}
					edge.edge_re.spfa_1 = edge.edge_re.price;
				}
			}
		}	
		return true;
	}
	
	
	/**
	 * 算法初始化，读取数据，初始化各种全局变量
	 * @return 成功为true（实际没用）
	 */
	private boolean algorithmInit(String[] data ) {
		String[] inline = data[0].split(" ");
		node_num = Integer.parseInt(inline[0]);
		edge_num = Integer.parseInt(inline[1]);
		tar_node_num = Integer.parseInt(inline[2]);
		
		per_server_cost = Integer.parseInt(data[2]);
		
		node_preAndcost = new SpfaStruct[node_num + 2];  //初始化SPFA的标记数组
		
		queue_not_contains = new boolean[node_num + 2];
		
		/********************** 第一，初始化有向图 ************************/
		for ( int num = 0 ; num < node_num + 2 ; num++ ) {  //初始化 有向图 new 对象
			HashMap<Integer,Edge> init1 = new HashMap<>();
			HashMap<Integer,Edge> init2 = new HashMap<>();
			digraph_map.add(init1);
			digraph_map_best.add(init2);
			
			node_preAndcost[num] = new SpfaStruct();
		}
		
		super_src = node_num;  //记下超级源点超级汇点的位置
		super_tar = super_src + 1;
		
		int us, vs;     // 边  u -> v
		for ( int edge_index = 4 ; edge_index < edge_num + 4 ; edge_index++ ) {  //存入边，同时存入两个图中	
			int edge_capacity = 0;
			int edge_cost = 0;
			inline = data[edge_index].split(" ");
			
			us = Integer.parseInt(inline[0]);
			vs = Integer.parseInt(inline[1]);
			edge_capacity = Integer.parseInt(inline[2]);
			edge_cost = Integer.parseInt(inline[3]);					
			
			Edge u_v = new Edge( us, vs, edge_capacity, edge_cost, 0, edge_cost, MY_MAX, true, null);
			Edge v_u = new Edge( vs, us, edge_capacity, edge_cost, 0, edge_cost, MY_MAX, true, null);
			u_v.edge_re = v_u;
			v_u.edge_re = u_v;
			
			digraph_map.get(us).put( vs, u_v);
			digraph_map.get(vs).put( us, v_u);	
			
			Edge u_v1 = new Edge( us, vs, edge_capacity, edge_cost, 0, edge_cost, MY_MAX, true, null);
			Edge v_u1 = new Edge( vs, us, edge_capacity, edge_cost, 0, edge_cost, MY_MAX, true, null);
			u_v1.edge_re = v_u1;
			v_u1.edge_re = u_v1;

			digraph_map_best.get(us).put( vs, u_v1);
			digraph_map_best.get(vs).put( us, v_u1);
					
		}
		
		int real_tar , tar_node , need;    //需求点需求量初始化
		for ( int tar_index = edge_num + 5 ; tar_index < edge_num + 5 + tar_node_num ; tar_index++ ) {
			inline = data[tar_index].split(" ");
			real_tar = Integer.parseInt(inline[0]);
			tar_node = Integer.parseInt(inline[1]);
			need = Integer.parseInt(inline[2]);
			
			tars_need.put( tar_node, need);
			tars_real_number.put( tar_node, real_tar);
		}
		
		//这里更改，固定超级汇点！！！ 超级汇点连接所有需求节点！！！ 不需要改动！！！
		for ( int tar : tars_need.keySet() ) {
			     
			 //指向超级汇点，设置为1 （用于统计总流量）
			Edge tar_suptar = new Edge( tar, super_tar, tars_need.get(tar), 0, 0, 0, MY_MAX, true, null );
			Edge suptar_tar = new Edge( super_tar, tar, 0, MY_MAX, 0, MY_MAX, MY_MAX, true, null );
			tar_suptar.edge_re = suptar_tar;
			suptar_tar.edge_re = tar_suptar;
			
			digraph_map.get(tar).put( super_tar, tar_suptar);
			digraph_map.get(super_tar).put( tar, suptar_tar);
			
			Edge tar_suptar1 = new Edge( tar, super_tar, tars_need.get(tar), 0, 0, 0, MY_MAX, true, null );
			Edge suptar_tar1 = new Edge( super_tar, tar, 0, MY_MAX, 0, MY_MAX, MY_MAX, true, null );
			tar_suptar1.edge_re = suptar_tar1;
			suptar_tar1.edge_re = tar_suptar1;

			digraph_map_best.get(tar).put( super_tar, tar_suptar1);
			digraph_map_best.get(super_tar).put( tar, suptar_tar1);
		
		}
		
		return true;
	}
	
	/**
	 * 用于添加或者删除源时，更改图上的路径，一次只能加一个点或者删除一个点
	 * @param the_map 需要更改路径的图
	 * @param new_srcs 新的源
	 * @param delete_srcs 需要删除的源
	 */
	private void buildEdge(ArrayList<HashMap<Integer, Edge>> the_map, int new_srcs, int delete_srcs) {
		if ( delete_srcs != -1 ) {  //删除老的服务器点 以及它的边
			the_map.get(delete_srcs).remove(super_src);
			the_map.get(super_src).remove(delete_srcs);
		}
		
		if ( new_srcs != -1 ) {   //添加新的服务器点 需要加入边	
			Edge supsrc_ns = new Edge( super_src, new_srcs, MY_MAX, 0, 0, 0, MY_MAX, true, null);
			Edge ns_supsrc = new Edge( new_srcs, super_src, 0, MY_MAX, 0, MY_MAX, MY_MAX, true, null);
			supsrc_ns.edge_re = ns_supsrc;
			ns_supsrc.edge_re = supsrc_ns;
			
			the_map.get(super_src).put( new_srcs, supsrc_ns);
			the_map.get(new_srcs).put( super_src, ns_supsrc);
		}
		
	}
	
	/**
	 * 用于更改 srcs_now集合 使它在 srcs_best集合 基础上添加或者删除源
	 * @param new_srcs 需要添加的源
	 * @param delete_srcs 需要删除的源
	 */
	private void update_srcs_now(int new_srcs, int delete_srcs) {	
		srcs_now.clear();
		srcs_now.addAll(srcs_best);
		if ( delete_srcs != -1 ) {
			srcs_now.remove((Integer)delete_srcs );
		}
		if ( new_srcs != -1 ) {
			srcs_now.add(new_srcs);
		}
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
		srcs_best.clear();
		srcs_best.addAll(srcs_now);
	}
	
	/** 
	 * 写出最后的结果到文件
	 * @return 导出digraph_map_best中的流，变成一行的路径，用于输出
	 */
	public String[] writePath() {
		
		LinkedList<String> the_path_str = new LinkedList<>(); 
		int costt = 0;
		int floww = 0;
		for	( int src : srcs_best ) {
			Collection<Edge> edges = digraph_map_best.get(src).values();
			for ( Edge edge : edges ) {
				while ( edge.flow != 0 ) {
					String str = edge.pre + " ";
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
					floww += delete;
					for ( Edge to_adjust : path ) {
						costt += delete * to_adjust.price;
						to_adjust.flow -= delete;
						if ( to_adjust.next != super_tar ) {
							str += to_adjust.next + " ";
						} else {
							str += tars_real_number.get(to_adjust.pre) + " " + delete;
						}
					}
					the_path_str.add(str);
				}
			}
		}
		
		String[] arr_to_write = new String[the_path_str.size() + 2];
		arr_to_write[0] = "" + the_path_str.size();
		arr_to_write[1] = "";
		for ( int tw = 0 ; tw < the_path_str.size() ; tw++ ) {
			arr_to_write[ tw + 2 ] = the_path_str.get(tw);
		}
		costt += per_server_cost * srcs_best.size();
		System.out.println("\n检测费用：" + costt + "  检测流量：" + floww);
		return arr_to_write;
	}

}

