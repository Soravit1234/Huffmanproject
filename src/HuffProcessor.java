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

		int[] counts = readForCounts(in, out);  // added out for out. methods
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);

		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);

		in.reset();
		writeCompressedBits(codings,in,out);
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
	
	private int[] readForCounts(BitInputStream in, BitOutputStream out) {
		// helper method to find frequency
		int[] store257 = new int[ALPH_SIZE + 1];
		store257[PSEUDO_EOF] = 1;
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}
		out.close();
		return store257;
	}

	private HuffNode makeTreeFromCounts(int[] counts) {
		// helper method to make coding from tree
		PriorityQueue<HuffNode> pq = new PriorityQueue<HuffNode>();

		for (int a=0; a<counts.length; a++) {
			if (counts[a]>0) {
				pq.add(new HuffNode(a, counts[a], null, null));
			}
		}
		while (pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(0, left.myWeight+right.myWeight, left, right);
			pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}

	private String[] makeCodingsFromTree(HuffNode root) {
		// helper method to make coding from tree
		String[] encodings = new String[ALPH_SIZE + 1];
		codingHelper(root, "", encodings);	// ANOTHER HELPER METHOD
		return encodings;	
	}

	private void codingHelper(HuffNode root, String path, String[] encodings) {
		if (root.myValue == 1) {
			encodings[root.myValue] = path;
			return;
		}
		else {
			codingHelper(root.myLeft, path+"0", encodings);
			codingHelper(root.myRight, path+"1", encodings);
		}
	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		if (root.myValue == 0) {
			out.writeBits(1, 0);
			writeHeader(root.myLeft,out);
			writeHeader(root.myRight,out);
		} else {
			String holdMe = "1" + String.valueOf(root.myValue);
			int holdStr = Integer.valueOf(holdMe);
			out.writeBits(1+BITS_PER_WORD+1, holdStr);
		}
	}

	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		// WRITE HELPER METHOD
		for (int a=0; a<codings.length; a++) {
			String code = codings[a];
			out.writeBits(code.length(), Integer.parseInt(code));
		}
		String code = codings[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code));
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
	
	
	
	
	
	
	
	
	
	
	
	
	