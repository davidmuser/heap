 * Heap
 *
 * An implementation of Fibonacci heap over positive integers
 * with the possibility of not performing lazy melds and
 * the possibility of not performing lazy decrease keys.
 */
public class Heap
{
    public final boolean lazyMelds;
    public final boolean lazyDecreaseKeys;

    // Pointers and counters
    public HeapItem min; 
    public HeapNode firstTree;
    public int size;
    public int numTrees;
    public int numMarkedNodes;
    public int totalLinks;
    public int totalCuts;
    public int totalHeapifyCosts;

    /**
     * Constructor to initialize an empty heap.
     * Time Complexity: O(1)
     */
    public Heap(boolean lazyMelds, boolean lazyDecreaseKeys)
    {
        this.lazyMelds = lazyMelds;
        this.lazyDecreaseKeys = lazyDecreaseKeys;
        this.min = null;
        this.firstTree = null;
        this.size = 0;
        this.numTrees = 0;
        this.numMarkedNodes = 0;
        this.totalLinks = 0;
        this.totalCuts = 0;
        this.totalHeapifyCosts = 0;
    }

    /**
     * pre: key > 0
     * Insert (key,info) into the heap and return the newly generated HeapItem.
     * Time Complexity: O(1) if lazy, O(log n) worst-case if not lazy (due to consolidation).
     */
    public HeapItem insert(int key, String info)
    {
        // 1. Create the data item
        HeapItem newItem = new HeapItem();
        newItem.key = key;
        newItem.info = info;

        // 2. Create the structural node
        HeapNode newNode = new HeapNode();
        newNode.item = newItem;
        
        // 3. Link back
        newItem.node = newNode;

        // 4. Meld logic
        Heap tempHeap = new Heap(this.lazyMelds, this.lazyDecreaseKeys);
        tempHeap.firstTree = newNode;
        tempHeap.min = newItem;
        tempHeap.size = 1;
        tempHeap.numTrees = 1;

        meld(tempHeap);

        return newItem;
    }

    /**
     * Return the minimal HeapItem, null if empty.
     * Time Complexity: O(1)
     */
    public HeapItem findMin()
    {
        return this.min;
    }

    /**
     * Delete the minimal item.
     * Time Complexity: O(n) worst-case (scanning roots)
     */
    public void deleteMin()
    {
        if (this.min == null) {
            return;
        }

        HeapNode z = this.min.node;

        // 1. Remove z from the root list of THIS heap
        removeNodeFromRootList(z);
        this.size--;
        this.numTrees--;

        // Reset min temporarily
        if (this.size == 0) {
            this.min = null;
            this.firstTree = null;
        } else {
            // Point min to an arbitrary node in the root list if available,
            // actual min will be found during consolidate/meld.
            if (this.firstTree != null) {
                this.min = this.firstTree.item;
            }
        }

        // 2. Prepare the children as a new Heap to be melded
        if (z.child != null) {
            Heap childHeap = new Heap(this.lazyMelds, this.lazyDecreaseKeys);
            HeapNode firstChild = z.child;

            // Disconnect children from z and find min among them
            int childCount = 0;
            HeapItem minChildItem = firstChild.item;
            HeapNode curr = firstChild;

            do {
                curr.parent = null; // Disconnect from z
                childCount++;
                if (curr.item.key < minChildItem.key) {
                    minChildItem = curr.item;
                }
                curr = curr.next;
            } while (curr != firstChild);

            childHeap.firstTree = firstChild;
            childHeap.min = minChildItem;
            childHeap.size = 0; 
            childHeap.numTrees = childCount;

            meld(childHeap);
        }

        // 3. Consolidate (always required in deleteMin)
        consolidate();

        // Safe check for empty heap after consolidate
        if (this.size == 0) {
            this.min = null;
        }
    }

    /**
     * pre: 0<=diff<=x.key
     * Decrease the key of x by diff and fix the heap.
     * Time Complexity: O(log n) worst-case (cascading cuts or heapifyUp).
     */
    public void decreaseKey(HeapItem x, int diff)
    {
        x.key -= diff;
        HeapNode node = x.node;

        if (node.parent != null && node.item.key < node.parent.item.key) {
            if (this.lazyDecreaseKeys) {
                HeapNode parent = node.parent;
                cut(node, parent);
                cascadingCut(parent);
            } else {
                heapifyUp(node);
            }
        }

        if (this.min == null || x.key < this.min.key) {
            this.min = x;
        }
    }

    /**
     * Delete the x from the heap.
     * Time Complexity: O(n) worst-case (calls deleteMin).
     */
    public void delete(HeapItem x)
    {

        int diff = x.key +2;
        decreaseKey(x, diff);
        deleteMin();
    }

    /**
     * Meld the heap with heap2.
     * Time Complexity: O(1) if lazy, O(n) worst-case if not lazy (consolidate).
     */
    public void meld(Heap heap2)
    {
        if (heap2 == null || heap2.firstTree == null) {
            return;
        }

        // Merge state
        this.size += heap2.size;
        this.totalLinks += heap2.totalLinks;
        this.totalCuts += heap2.totalCuts;
        this.totalHeapifyCosts += heap2.totalHeapifyCosts;
        this.numMarkedNodes += heap2.numMarkedNodes;

        // Concatenate root lists
        if (this.firstTree == null) {
            this.firstTree = heap2.firstTree;
            this.min = heap2.min;
            this.numTrees = heap2.numTrees;
        } else {
            HeapNode last1 = this.firstTree.prev;
            HeapNode last2 = heap2.firstTree.prev;

            last1.next = heap2.firstTree;
            heap2.firstTree.prev = last1;
            last2.next = this.firstTree;
            this.firstTree.prev = last2;

            this.numTrees += heap2.numTrees;

            if (heap2.min.key < this.min.key) {
                this.min = heap2.min;
            }
        }

        // Clear heap2
        heap2.firstTree = null;
        heap2.min = null;
        heap2.size = 0;
        heap2.numTrees = 0;
        heap2.numMarkedNodes = 0;

        // Check if strict meld is required
        if (!this.lazyMelds) {
            consolidate();
        }
    }

    /**
     * Return the number of elements in the heap
     */
    public int size()
    {
        return this.size;
    }

    /**
     * Return the number of trees in the heap.
     */
    public int numTrees()
    {
        return this.numTrees;
    }

    /**
     * Return the number of marked nodes in the heap.
     */
    public int numMarkedNodes()
    {
        return this.numMarkedNodes;
    }

    /**
     * Return the total number of links.
     */
    public int totalLinks()
    {
        return this.totalLinks;
    }

    /**
     * Return the total number of cuts.
     */
    public int totalCuts()
    {
        return this.totalCuts;
    }

    /**
     * Return the total heapify costs.
     */
    public int totalHeapifyCosts()
    {
        return this.totalHeapifyCosts;
    }

    // --- Private Helper Functions ---
 // 
    private void consolidate() {
        if (this.firstTree == null || this.size == 0) return;

        // Calculate array size based on max degree approx O(log n)
        int capacity = 60; 
        
        // Dynamic sizing protection (optional but good practice)
        int estimatedRank = (int)(Math.log(this.size) / Math.log(1.618)) + 5;
        if (estimatedRank > capacity) capacity = estimatedRank;

        HeapNode[] A = new HeapNode[capacity];

        // Iterate over current roots
        int count = this.numTrees;
        HeapNode[] roots = new HeapNode[count];
        HeapNode curr = this.firstTree;
        for (int i = 0; i < count; i++) {
            roots[i] = curr;
            curr = curr.next;
        }

        for (int i = 0; i < count; i++) {
            HeapNode x = roots[i];
            
            int d = x.rank;
            while (d < A.length && A[d] != null) {
                HeapNode y = A[d];

                // Ensure x has the smaller key
                if (x.item.key > y.item.key) {
                    HeapNode temp = x;
                    x = y;
                    y = temp;
                }

                link(y, x);
                A[d] = null;
                d++;
            }
            if (d < A.length) {
                A[d] = x;
            }
        }

        // Reconstruct the root list from A
        this.min = null;
        this.firstTree = null;
        this.numTrees = 0;

        for (int i = 0; i < A.length; i++) {
            if (A[i] != null) {
                if (this.firstTree == null) {
                    this.firstTree = A[i];
                    this.firstTree.next = this.firstTree;
                    this.firstTree.prev = this.firstTree;
                    this.min = A[i].item;
                } else {
                    HeapNode last = this.firstTree.prev;
                    last.next = A[i];
                    A[i].prev = last;
                    A[i].next = this.firstTree;
                    this.firstTree.prev = A[i];

                    if (A[i].item.key < this.min.key) {
                        this.min = A[i].item;
                    }
                }
                this.numTrees++;
            }
        }
    }

    private void link(HeapNode y, HeapNode x) {
        // y becomes child of x
        y.parent = x;
        if (x.child == null) {
            x.child = y;
            y.next = y;
            y.prev = y;
        } else {
            y.next = x.child;
            y.prev = x.child.prev;
            x.child.prev.next = y;
            x.child.prev = y;
            x.child = y;
        }
        x.rank++;

        if (y.mark) {
            y.mark = false;
            this.numMarkedNodes--;
        }
        this.totalLinks++;
    }

    private void cut(HeapNode x, HeapNode y) {
        x.parent = null;
        y.rank--;
        if (x.next == x) {
            y.child = null;
        } else {
            x.prev.next = x.next;
            x.next.prev = x.prev;
            if (y.child == x) {
                y.child = x.next;
            }
        }

        this.totalCuts++;
        if (x.mark) {
            this.numMarkedNodes--;
            x.mark = false;
        }

        // Reset sibling pointers for x to make it a valid single-node list
        x.next = x;
        x.prev = x;

        // Use meld to add back to root list
        Heap tempHeap = new Heap(this.lazyMelds, this.lazyDecreaseKeys);
        tempHeap.firstTree = x;
        tempHeap.min = x.item;
        tempHeap.numTrees = 1;
        tempHeap.size = 0; // Avoid double counting size

        meld(tempHeap);
    }

    private void cascadingCut(HeapNode y) {
        HeapNode z = y.parent;
        if (z != null) {
            if (!y.mark) {
                y.mark = true;
                this.numMarkedNodes++;
            } else {
                cut(y, z);
                cascadingCut(z);
            }
        }
    }

    private void heapifyUp(HeapNode node) {
        while (node.parent != null && node.item.key < node.parent.item.key) {
            HeapNode parentNode = node.parent;

            // Retrieve items
            HeapItem itemNode = node.item;
            HeapItem itemParent = parentNode.item;

            // Swap items references in nodes (Pointer swapping)
            node.item = itemParent;
            parentNode.item = itemNode;

            // Update nodes references in items
            itemNode.node = parentNode;
            itemParent.node = node;

            // Move pointer up
            node = parentNode;

            this.totalHeapifyCosts++;
        }
    }

    private void removeNodeFromRootList(HeapNode z) {
        if (z.next == z) {
            this.firstTree = null;
        } else {
            z.prev.next = z.next;
            z.next.prev = z.prev;
            if (this.firstTree == z) {
                this.firstTree = z.next;
            }
        }
        // Isolate z
        z.next = z;
        z.prev = z;
    }

    /**
     * Class implementing a node in a Heap.
     */
    public static class HeapNode{
        public HeapItem item;
        public HeapNode child;
        public HeapNode next;
        public HeapNode prev;
        public HeapNode parent;
        public int rank;
        public boolean mark; 

        public HeapNode() {
            this.rank = 0;
            this.mark = false;
            this.next = this;
            this.prev = this;
            this.parent = null;
            this.child = null;
        }
    }

    /**
     * Class implementing an item in a Heap.
     */
    public static class HeapItem{
        public HeapNode node;
        public int key;
        public String info;
    }
}