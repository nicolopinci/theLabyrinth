import java.util.*;
import java.io.*;
import java.math.*;

class Coordinate {
    private int x;
    private int y;
    
    public Coordinate(int x, int y) { // defines a coordinate (x, y)
        this.x = x;
        this.y = y;
    }
    
    public int getX() { 
        return this.x;
    }
    
    public int getY() {
        return this.y;
    }
    
    public int hashCode(){
        int hashcode = 0;
        hashcode = x+y*1000;
        return hashcode;
    }
    
    public boolean equals(Object obj){ // implements the equality for two elements of the coordinate class
        if (obj instanceof Coordinate) {
            Coordinate coo = (Coordinate) obj;
            return (coo.getY() == this.y && coo.getX() == this.x);
        } else {
            return false;
        }
    }
    
    public int distance(Object x) { // calculates the (Manhattan) distance between two coordinates
        if(x instanceof Coordinate) {
            Coordinate a = (Coordinate) x;
            return Math.abs(a.getX() - this.getX()) + Math.abs(a.getY() - this.getY());
        }
        return -1;
    }
    
    public double correctedDistance(Object x) {
        if(x instanceof Coordinate) {
            Coordinate a = (Coordinate) x;
            return Math.abs(a.getX() - this.getX()) + Math.abs(a.getY() - this.getY())+0.2*Math.signum(a.getX() - this.getX())+0.2*Math.signum(a.getY() - this.getY());
        }
        return -1;
    }
    
    public String toString() { // can bse used to print a coordinate, mainly for debug purposes
        return "<x, y> = <" + this.getX() + ", " + this.getY() + ">";
    }
}

class Player {

    private static Vector<String> matrix = new Vector<String>(); // the game field, containing the labyrinth
    private static Vector<Coordinate> questionMarks = new Vector<Coordinate>(); // question marks locations
    
    private static Coordinate controlRoom = new Coordinate(-1,-1); 
    // the position of the control room
    // Note: the initial coordinate of the control room is (-1, -1) since all the positions inside the labyrinth
    // are positive
    
    private static boolean runAway = false;
    // runAway becomes true when the control room has been reached and
    // the countdown has started. In such a situation Kirk has to find the
    // best path to reach the starting position
    
    private static Coordinate startingPoint = new Coordinate(-1,-1);
    // The starting point is also inizialized using (-1,-1), but its value
    // is changed immediately when the game loop starts
    
    private static Vector<Coordinate> pathToC = new Vector<Coordinate>();
    // pathToC is a vector that contains the path to the control room
    
    // pathFromHere is used to store the path from a given position to
    // the control room
    private static Vector<Coordinate> pathFromHere = new Vector<Coordinate>();

    // flag that will be used to determine the correct behaviour
    // when the control room is discovered and reached
    private static boolean firstFindC = true;
    
    // cumQM is used to store the total number of question marks found along
    // a path. This is useul because in this way the best choice can be selected,
    // when the control room cannot be reached
    private static HashMap<Coordinate, Integer> cumQM = new HashMap<Coordinate, Integer>();
    
    // distPath is used to store the distance of a node from the source node
    // during a research. It would be possible to introduce a correction that
    // gives a different weight to the nodes that Kirk has already visited/discovered
    private static HashMap<Coordinate, Integer> distPath = new HashMap<Coordinate, Integer>();
    
    private static Vector<Coordinate> visitedNodes = new Vector<Coordinate>();
    // visitedNodes contains the list of all the nodes Kirk has visited so far

    private static void printMatrix() {
        for(String row:matrix) {
            System.err.println(row);
        }
    }
    
    private static char what(Coordinate xy) {
        try {
            char out = matrix.get(xy.getY()).charAt(xy.getX());
            return out;
        }
        catch(Exception e) {
            // If the coordinate is outside of the field, then
            // it is equivalent to a wall, since it cannot be reached
            return '#';
        }
    }
    
    private static boolean isControlRoom(Coordinate position) {
        return what(position) == 'C';
    }
    
     private static boolean isHollow(Coordinate position) {
        return what(position) == '.' || what(position) == 'T';
    }
    
    public static Coordinate seeRight(Coordinate coor) {
        int x = coor.getX();
        int y = coor.getY();
        Coordinate out = new Coordinate(x+1, y); // right = increase the x coordinate
        return out;
    }
    
    public static Coordinate seeLeft(Coordinate coor) {
        int x = coor.getX();
        int y = coor.getY();
        Coordinate out = new Coordinate(x-1, y); // left = decrease the x coordinate
        return out;   
    }
    
    public static Coordinate seeUp(Coordinate coor) {
        int x = coor.getX();
        int y = coor.getY();
        Coordinate out = new Coordinate(x, y-1); // up = decrease the y coordinate
        return out;    
    }
    
    public static Coordinate seeDown(Coordinate coor) {
        int x = coor.getX();
        int y = coor.getY();
        Coordinate out = new Coordinate(x, y+1); // down = decrease the y coordinate
        return out;
    }
    
    private static void moveKirk(Coordinate from, Coordinate to) {

        visitedNodes.add(from);
        visitedNodes.add(to);

         if(from.getX() == to.getX()) { // Kirk moves vertically

            if(from.getY() == to.getY() + 1) {
                System.out.println("UP");
            }
            else if(from.getY() == to.getY() - 1) {
                System.out.println("DOWN");
            }
         }
        else if(from.getY() == to.getY()) { // Kirk moves horizontally
            if(from.getX() == to.getX() + 1) {
                System.out.println("LEFT");
            }
            else if(from.getX() == to.getX() - 1) {
                System.out.println("RIGHT");
            }
        }
    }
    
    private static Vector<Coordinate> canBeWalked(int R, int C) {
        // This method returns a vector containing all the cells on which
        // it is possible to walk, since they are hollow (or the starting position)
        Vector<Coordinate> walkable = new Vector<Coordinate>();
        for(int i=0; i<R; ++i) {
            for(int j=0; j<C; ++j) {
                if(isHollow(new Coordinate(j, i))) {
                    walkable.add(new Coordinate(j, i));
                }
            }
        }
        return walkable;
    }
    
    
    private static Vector<Coordinate> getQuestionNeighbours(Coordinate position) {
        Vector<Coordinate> neighbours = new Vector<Coordinate>();
                
         for(int i=position.getX()-1;i<=position.getX()+1;++i) {
            for(int j=position.getY()-1;j<=position.getY()+1;++j) {
                if(position.distance(new Coordinate(i, j))==1) {
                    if(isQuestion(new Coordinate(i, j))) {
                        neighbours.add(new Coordinate(i, j));
                    }
                }
            }
        }
        return neighbours;
    }
    
    private static Vector<Coordinate> getExtendedQuestionNeighbours(Coordinate position) {
        Vector<Coordinate> neighbours = new Vector<Coordinate>();
                
         for(int i=position.getX()-2;i<=position.getX()+2;++i) {
            for(int j=position.getY()-2;j<=position.getY()+2;++j) {
                    if(isQuestion(new Coordinate(i, j))) {
                        neighbours.add(new Coordinate(i, j));

                }
            }
        }
        
        return neighbours;
    }
    
    private static Vector<Coordinate> getWalkableNeighbours(Coordinate position) {
        Vector<Coordinate> neighbours = new Vector<Coordinate>();
                
        for(int i=position.getX()-1;i<=position.getX()+1;++i) {
            for(int j=position.getY()-1;j<=position.getY()+1;++j) {
                if(position.distance(new Coordinate(i, j))==1) {
                    if(isHollow(new Coordinate(i, j))) {
                        neighbours.add(new Coordinate(i, j));
                    }
                }
            }
        }  
        return neighbours;
    }
 
    private static Map<Coordinate, Coordinate> BFS(Coordinate source, int R, int C, boolean considerC) {
        
        // Breadth-first-search algorithm to find the best path from a position
        // to all the others
        
        // Returns the distances from the source to all the other reachable cells
        // as a list of predecessors
        
        Map<Coordinate, Coordinate> predecessors = new HashMap<Coordinate, Coordinate>();
        
        // adjacent is used to store the adjacent vertices with respect to a
        // given vertex (the position under analysis at a given moment)
        Vector<Coordinate> adjacent = new Vector<Coordinate>();
        
        // This data structure is used to associate a flag to every coordinate
        // that can be possibily visited (the hollow cells)
        // Once a cell has been considered, its flag goes to true
        HashMap<Coordinate, Boolean> flag = new HashMap<Coordinate, Boolean>();
        
        // The queue used by the BFS algorithm
        Queue<Coordinate> Q = new LinkedList<Coordinate>();
        
        // This is the vector containing all the coordinates that represent
        // hollow cells, independently from the fact that they are reachable.
        // In a graph representation the elements of this vector are the
        // nodes of the graph
        Vector<Coordinate> canBeWalked = new Vector<Coordinate>();
        
        // The vector containing the cells that have been visited by the BFS
        // algorithm (not necessarily by Kirk). The set of cells visited by BFS
        // is a subset of the hollow cells whose content is known, but there is the possibiliy
        // that Kirk hasn't visited directly the considered cells.
        Vector<Coordinate> visited = new Vector<Coordinate>();
 
        cumQM.clear();
        distPath.clear();
        
        // Get all the hollow cells discovered so far
        canBeWalked = canBeWalked(R, C);
        
         if(considerC) { // if the control room has already been visited
            canBeWalked.add(controlRoom);
        }
        // Note: in case the control room has already been visited it is always not convenient
        // for Kirk to go there another time, since a loop would occur. However, this coordinate
        // has been considered since Kirk starts from there, and therefore it is part of his
        // best path
        
        for(Coordinate v:canBeWalked) {
            flag.put(v, false);
            
            // A position v can have some predecessors, that allow to reconstruct
            // the best path found by BFS. Before the actual execution of BFS, however,
            // there aren't paths and, therefore, there aren't predecessors (yet)
            predecessors.put(v, null);
        }
                
        flag.put(source, true); // Since Kirk starts from the source, it has been visited
        Q.add(source); // add the source to the BFS queue
        
        // associate the number of question marks nearby to the source node
        cumQM.put(source, getExtendedQuestionNeighbours(source).size()); 
        
        // The distance of the source from itself is 0
        distPath.put(source, 0);
        
        while(!Q.isEmpty()) { // Not all the reachable positions have been considered yet
            Coordinate v = Q.poll(); // extract the position that entered the queue first
            adjacent = getWalkableNeighbours(v); // get the adjacent positions that can be visited
             if(considerC && (isControlRoom(seeDown(v)) || isControlRoom(seeUp(v)) || isControlRoom(seeRight(v)) || isControlRoom(seeLeft(v)))) {
                adjacent.add(controlRoom); // and add the control room if allowed
            }
                        
            for(Coordinate w:adjacent) { // for every adjacent position
                if(flag.get(w) == false) { // if that position has not been visited yet

                    flag.put(w, true); // now it has been visited
                    predecessors.put(w, v); // its predecessor is the last position having been extracted
                    Q.add(w); // this new position can be added to the queue
                    
                    // associate to that position a value that is the sum of the 
                    // predecessor value and the question marks nearby
                    cumQM.put(w, cumQM.get(v) + getExtendedQuestionNeighbours(w).size()); 
                    
                    // the distance from the source is determined by the predecessor's
                    // distance and by the fact that the node had been visited (by Kirk!)
                    // previously
                    distPath.put(w, distPath.get(v) + 1 + visitedNode(v));
                    
                    // Note: visitedNode can be used to change the distance on the basis 
                    // of Kirk's previous behaviour, but in this implementation visitiedNode
                    // always return 0. It has been kept in the code for any future implementation
                    // and/or optimization
                }
            }
        }
        return predecessors;

    }
    
    private static int visitedNode(Coordinate v) {
        for(Coordinate c:visitedNodes) {
            if(v.equals(c)) {
                return 0;
            }
        }
        return 0;
    }
    
    private static boolean isQuestion(Coordinate coor) {
        return what(coor) == '?';
    }
    
    private static void findQuestionMarks(int R, int C) {
        for(int i=0; i<R; ++i) {
            for(int j=0; j<C; ++j) {
                if(isQuestion(new Coordinate(j, i))) {
                    questionMarks.add(new Coordinate(j, i));
                }
            }
        }
    }

    private static boolean isBorderDot(Coordinate coor) {
        Vector<Coordinate> neighbours = getQuestionNeighbours(coor);
        if(neighbours.size()==0) {
            return false;
        }
        else {
            return true;
        }
    }
    
    private static Vector<Coordinate> findBestChoice(Coordinate currentPosition, int R, int C) {
        
        // This method returns the path to the best choice from the current position
        // The best choice is determined by considering some parameters related to the 
        // number of question marks and to the distance.
        // In particular, the goal is to maximize the number of discovered question
        // marks in the shortest time possible (=> shortest distance traveled bt Kirk)
        
        // List of predecessors given a position
        Map<Coordinate, Coordinate> pr = new HashMap<Coordinate, Coordinate>();
        
        // Path from the current position to the best choice
        Vector<Coordinate> path = new Vector<Coordinate>();
        
        // The best choice hasn't been found yet
        boolean found = false;
        
        // The parameter used to determine the quality of a choice is initially
        // set to - infinity, since the highest value corresponds to the best choice
        Float tempNum = Float.NEGATIVE_INFINITY;

        // The best choice is outside the field, but it will be set during the fist iteration
        Coordinate bestChoice = new Coordinate(-1, -1);
        
        // Gets the list of predecessors reachable from the current position
        pr = BFS(currentPosition, R, C, false);
                
        for (Map.Entry<Coordinate, Coordinate> entry : pr.entrySet()) {
            if(entry.getValue() != null) {
                // A border dot is an hollow cell that has at least one
                // question mark as a neighbour. It is part of the fringe, if
                // this structured is considered as a graph
                if(isBorderDot(entry.getKey())) {
                    // Calculation of the "score" related to the considered cell
                    float num = cumQM.get(entry.getKey())-12*distPath.get(entry.getKey());

                    // See if this score is better than the previous highest score...
                    if(num > tempNum) {
                         // ...and in case update the highest score and 
                         // the best choice coordinates
                        bestChoice = entry.getKey();
                        tempNum = num;
                    }
                }
            }
        }
        
        // Now, given the best choice, find the path to it
        Coordinate pathElement = bestChoice;
        while(!found) {
            path.add(pathElement);
            pathElement = pr.get(pathElement);
            // Note: it is possible to use == because equals has been implemented
            // in the Coordinate class
            if(pathElement == currentPosition) {
                found = true;
            }
        }
        path.add(currentPosition);
        return path;
    }
    
    private static boolean isThereC(int R, int C) {
        // This methods checks if the control room location has been
        // found by Kirk (this does not necessarily mean that such a 
        // position is reachable by Kirk)
        for(int i=0; i<R; ++i) {
            for(int j=0; j<C; ++j) {
                if(isControlRoom(new Coordinate(j, i))) {
                    controlRoom = new Coordinate(j, i);
                    return true;
                }
            }
        }
        return false;
    }
    
    private static Vector<Coordinate> findRoute(Coordinate start, int R, int C) {
        
                // Given a position start, this methods returns the path from
                // the control room to that position, if such a position exists
                
                // The output is given as a vector representing a sequence of
                // adjacent positions from start to the control room or null in
                // case such a sequence does not exist
        
                // Find the list of all the predecessors to all the destinations
                Map<Coordinate, Coordinate> predecC = BFS(start, R, C, true);
                
                // Vector containing the shortest path from the control room to
                // the coordinate "start"
                Vector<Coordinate> ptc = new Vector<Coordinate>();
                
                // Find the node before the control room in the path starting from
                // the "start" coordinate
                Coordinate predecessor = predecC.get(controlRoom);
                
                // Add to the path to the control room the control room itself...
                ptc.add(controlRoom);
                
                // ... and its predecessor
                ptc.add(predecessor);
                
                boolean found = false;
                while(!found) {
                    predecessor = predecC.get(predecessor);
                    ptc.add(predecessor);
                    
                    // When the starting point is reached, then the goal has been reached
                    // so found = true
                    if(predecessor == start) {
                        found = true;
                    }
                    
                    // If the predecessor is null, then the node is not reachable
                    if(predecessor == null) {
                        return null;
                    }
                }
               
                return ptc;
    }
    
    public static void main(String args[]) {
        
        Scanner in = new Scanner(System.in);
        int R = in.nextInt(); // number of rows.
        int C = in.nextInt(); // number of columns.
        
        int A = in.nextInt(); // number of rounds between the time the alarm countdown is activated and the time the alarm goes off.
        
        // game loop
        while (true) {
            
            int KR = in.nextInt(); // row where Kirk is located.
            int KC = in.nextInt(); // column where Kirk is located.
            
            // Note: the matrix is in fact a vector of strings and it represents the
            // game field, divided in cells. A cell, therefore, is represented by a 
            // character, while a string is a row of the matrix, that is an horizontal
            // 'slice' of the labyrinth
            
            matrix.clear();
            for (int i = 0; i < R; i++) {
                String ROW = in.next(); // C of the characters in '#.TC?' (i.e. one line of the ASCII maze).
                matrix.add(ROW);
            }
            
            // Set the current poition to the correct value
            Coordinate currentPosition = new Coordinate(KC, KR);
            
            boolean noPath = false;
            
            // Set the current position to the correct value
            if(what(currentPosition) == 'T') {
                startingPoint = currentPosition;
            }
                            
            if(runAway) { // the countdown has started
                pathToC.remove(0); // the first element is the current position
                
                // move to the next position along the path from the control room 
                // to the starting position T
                moveKirk(currentPosition, pathToC.get(0));
            }
            if(!runAway) { // the countdown has not started yet
                 if(isThereC(R, C)) { // the control room position has been found
                    if(firstFindC) {
                        // find the shortest route from C to the starting point
                        pathToC = findRoute(startingPoint, R, C); 
                    }
                                    
                    // If the path from C to the starting point exists 
                    // and if it is short enough
                    if(pathToC != null && pathToC.size() <= A+1) {

                            if(firstFindC) { // it is the first time that it is computed
                                // find the shortest path from the current position
                                pathFromHere = findRoute(currentPosition, R, C);
                                
                                // the next time won't be the first anymore
                                firstFindC = false;
                            }

                            if(pathFromHere.get(pathFromHere.size()-1) == currentPosition) {
                                pathFromHere.remove(pathFromHere.size()-1);
                            }
                            
                            // If Kirk is in the control room, he has to run away
                            if(isControlRoom(pathFromHere.get(pathFromHere.size()-1))) {
                                runAway = true;
                            }
                        
                            // Move Kirk to the next position
                            moveKirk(currentPosition, pathFromHere.get(pathFromHere.size()-1));
                            
                            // Remove the last position in the path
                            pathFromHere.remove(pathFromHere.size()-1);
                        
                    }
                    else { // otherwise ...
                        noPath = true; // a good enough path from C to T does not exist
                        firstFindC = true; // next time I have to look for such a path again
                    }
                }
                
                // In case the contro room position is unknown or
                // there isn't a good path from it to the starting position
                if(!isThereC(R, C) || noPath) {
                    // Determine the path to the best reachable cell
                    Vector<Coordinate> myPath = findBestChoice(currentPosition, R, C);  
                    
                    // Move Kirk along that path
                    moveKirk(currentPosition, myPath.get(myPath.size()-2));
                    
                    // Note: for each loop the best choice is determined again, since
                    // for every movement of Kirk the situation on the game field
                    // changes, since new cells are discovered
                }     
                
                printMatrix(); // shows the field status (for debug purposes, to visualize
                // the explored cells easily)
            }               
        }
    }
}
