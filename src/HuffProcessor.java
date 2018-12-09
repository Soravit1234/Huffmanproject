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
	public void compress(BitInputStream in, BitOutputStream out){

		int[] counts = readForCounts(in, out);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);


		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);

		in.reset();
		writeCompressedBits(codings, in , out);
		out.close();
	}
	
	public void decompress(BitInputStream in, BitOutputStream out){

		int bits = in.readBits(BITS_PER_INT);
		if(bits != HUFF_TREE) throw new HuffException("illegal header starts with" +bits);
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
		
	}
	
	//------------------------helper methods--------------------------------------//
	
	private int[] readForCounts(BitInputStream in, BitOutputStream out) {
		int[] storeed = new int[ALPH_SIZE + 1];
		storeed[PSEUDO_EOF] = 1;
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}
		out.close();
		return storeed;
	}
	
	private HuffNode makeTreeFromCounts(int [] cnts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		
		
		for(int i = 0; i < cnts.length; i ++) {
			if(cnts[i] > 0) 
				pq.add(new HuffNode(i, cnts[i]));
		}
		
		

		while (pq.size() > 1) {
		    HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    
		    // create new HuffNode t with weight from left.weight+right.weight and left, right subtrees
		    HuffNode t = new HuffNode(0, left.myWeight + right.myWeight, left, right);
		    pq.add(t);
		}
		
		HuffNode root = pq.remove();
		return root;
		
	}
	
	private String [] makeCodingsFromTree(HuffNode root) {
		String [] encodings = new String[ALPH_SIZE + 1];		
		encodingHelper(root, "", encodings);
		return encodings;
	}
	
	private void writeHeader(HuffNode root, BitOutputStream out) {
		
		if(root == null) return;
		if(root.myLeft == null && root.myRight == null) {
			out.writeBits(1,1);
			out.writeBits(BITS_PER_WORD + 1, root.myValue);
		}
		
		else {
			out.writeBits(1,0);
			writeHeader(root.myLeft, out);
			writeHeader(root.myRight, out);
		}	
	}
	
	private void encodingHelper(HuffNode root, String path, String[] encodings) {
		if (root.myLeft == null && root.myRight == null) {
			encodings[root.myValue] = path;
			if (myDebugLevel >= DEBUG_HIGH) {
				System.out.printf("encoding for %d is %s\n", root.myValue, path);
			}
			return;
		} else {
			encodingHelper(root.myLeft, path + "0", encodings);
			encodingHelper(root.myRight, path + "1", encodings);
		}
	}
	
	
	
	private void writeCompressedBits(String [] encoding, BitInputStream in, BitOutputStream out) {
	
		
		while (true) {
			int val = in.readBits(BITS_PER_WORD);
			
			if (val != -1) {
				String code = encoding[val];
			    out.writeBits(code.length(), Integer.parseInt(code,2));
				break;
			}
			
			else {
				String code = encoding[PSEUDO_EOF];
				out.writeBits(code.length(), Integer.parseInt(code,2));
			}
		}
		
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
	
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		// WRITE HELPER HERE PG 9
		HuffNode current = root;
		while (true) {
			int bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			} 
			else {
				if (bits == 0) {
					current = current.myLeft;
				} 
				else {
					current = current.myRight;
				}
				if (current.myValue == 1 || current.myValue == PSEUDO_EOF) {
					if (current.myValue == PSEUDO_EOF) {
						break;
					} 
					else {
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root;
					}
				}
			}
		}
	}


}
	
	
	
	
	
	
	
	
	
	
	
	
	