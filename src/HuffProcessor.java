import java.util.PriorityQueue;
/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out) {

		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);

		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);

		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();

	}
	
	public void decompress(BitInputStream in, BitOutputStream out) {

		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("Illegal header starts with " + bits);
		}

		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();

	}
	
	
	//------------------------helper methods--------------------------------------//
	
private String[] makeCodingsFromTree(HuffNode root) {
		
		String[] encodings = new String[ALPH_SIZE + 1];
	    doWork(root,"",encodings);
				
				
				return encodings;
			}
			
			private void doWork(HuffNode root, String path, String[] encodings) {
				if (root == null) return;
				
				if (root.myLeft == null && root.myRight == null) {
					encodings[root.myValue] = path;
			        return;
				}
				doWork(root.myLeft,path+"0", encodings);
				doWork(root.myRight,path+"1", encodings);
		
			}

	private int[] readForCounts(BitInputStream in) {
		int freq[] = new int[ALPH_SIZE+1];
		freq[PSEUDO_EOF] =1;
		while (true ) {
		int val = in.readBits(BITS_PER_WORD);
		if (val == -1) break;
		freq[val] = freq[val] +1;
		
		}
		return freq;
	}
	

	
	private HuffNode makeTreeFromCounts(int[] counts) {
		
		// TODO Auto-generated method stub
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();


		for(int dex = 0; dex < counts.length; dex ++) {
		    pq.add(new HuffNode(dex, counts[dex],null,null));
		}

		while (pq.size() > 1) {
		    HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    
		    // create new HuffNode t with weight from
		    // left.weight+right.weight and left, right subtrees
		    
		    HuffNode t = new HuffNode(-1,left.myWeight + right.myWeight, left, right);
		    pq.add(t);
		}
		
		HuffNode root = pq.remove();
		if (myDebugLevel >= DEBUG_HIGH) {
			System.out.printf("pq created with %d nodes\n", pq.size());
		}
		return root;
		
		
	}

	
	
//	private Object CodingsFromTree(HuffNode root) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		if(root.myLeft != null && root.myRight != null) {

			writeHeader(root.myLeft, out);

			writeHeader(root.myRight, out);

			out.writeBits(1, 0);			
		}

		else {

			out.writeBits(1, 1);

			out.writeBits(BITS_PER_WORD + 1, root.myValue);

		}
	}

	

	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		String code;
		
		while (true) {
		int val = in.readBits(BITS_PER_WORD);
		if (val == -1) { break;}
		code = codings[val];
		
		

		out.writeBits(code.length(), Integer.parseInt(code,2));
		}
		code = codings[PSEUDO_EOF];
	    out.writeBits(code.length(), Integer.parseInt(code,2));

	}

	private HuffNode readTreeHeader(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1)
			throw new HuffException("Could not read bit, value = -1");
		if (bit == 0) {
			return new HuffNode(0, 0, readTreeHeader(in), readTreeHeader(in));
		} else {
			return new HuffNode(in.readBits(BITS_PER_WORD + 1), 0, null, null);
		}
	}

	/*Reads the bits from the compressed file and uses them to traverse
	 root-to-leaf paths, writing leaf values to the output file. Stops when
	 PSEUDO_EOF is found*/
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		HuffNode current = root;
		while (true) {
			int bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO.EOF");
			} else {
				if (bits == 0)
					current = current.myLeft;
				else
					current = current.myRight;

				if (current.myLeft == null && current.myRight == null) {
					if (current.myValue == PSEUDO_EOF)
						break;
					else {
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root;
					}
				}
			}
		}
	}


}
	
	
	
	
	
	
	
	
	
	
	
	
	