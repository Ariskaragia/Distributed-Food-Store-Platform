# AndroidKatanemimena — Distributed Food/Store Platform (Android + Java, Master–Workers–Reducer)

A distributed store/food browsing platform consisting of an **Android client** and a **distributed Java backend** built with a **MasterServer + multiple WorkerServers + ReducerServer** architecture.

The system supports **Customer/Manager roles**, **geo-based search**, **filtering**, and a **MapReduce-style aggregation pipeline** where Workers push filtered results to a Reducer that **merges + sorts/ranks** the final list.

---

## Description

- Developed an Android application with Customer and Manager roles and role-based screens and permissions. 
- Implemented a distributed backend architecture with MasterServer, WorkerServers, and a ReducerServer. 
- Applied a Map/Reduce workflow: filtering and snapshot aggregation on workers, ranking and merging on the reducer. 
- Implemented replication and failover (primary/replica) with fallback when a primary worker is unavailable. 
- Added geo-based store search (Haversine distance) and filters by category, price range, and ratings. 

### Customer
- Sign-in / role-based navigation
- Browse stores with:
  - **Distance filtering** (Haversine)
  - Filters: **stars**, **food category**, **price category**
- Store details & product browsing
- Rate store (if enabled in your build)

### Manager
- Manage store catalog:
  - **Add / remove product**
  - **Update stock**
  - **Toggle product visibility**
- (Optional) sales/analytics queries (depending on your implementation)

---

## Architecture

### Components
- **Android App (Client)**
- **MasterServer**
  - Receives client requests
  - Routes to the appropriate Worker(s)
  - Handles worker liveness checks
- **WorkerServers (Map stage)**
  - Store subsets of stores/products
  - Execute request logic locally
  - **Filter results** (distance, stars, category, price)
  - **Push snapshots / intermediate results** to the Reducer
- **ReducerServer (Reduce stage)**
  - Receives intermediate results from Workers
  - **Merges / aggregates** all partial results
  - Applies **dedup + sorting/ranking** (final unified list)
  - Returns the final response back to MasterServer → Client

### MapReduce flow (high-level)
1. Client → MasterServer request
2. Master → Workers: query execution
3. Workers **filter + push** partial results to Reducer
4. Reducer **merge + sort/rank** → Master
5. Master → Client final response

---

## Replication & Failover (Backend)
- Stores are assigned using **hash-based mapping** to determine a **primary worker** and a **replica worker**.
- For **write operations**, if the primary worker is unavailable, the system **fails over** to the replica.
- Worker liveness is validated (e.g., alive checks) before routing.

---

## Tech Stack
- **Java** (backend), **Android SDK** (client)
- **TCP sockets** (ObjectInputStream/ObjectOutputStream)
- **Multithreading / Thread pools**
- **Gson** (serialization)
- **Glide** (image loading)
- **RecyclerView** (Android UI)

---

## Project Structure (typical)
> Adjust names if your folders differ.

- `AndroidKatanemimena/` (Android app)
- `MasterServer/`
- `WorkerServer/`
- `ReducerServer/`
- `ClusterLauncher/` (optional helper to start servers)

---

## How to Run (Local)

### 1) Start Backend Servers
Start servers in this order:

1. **ReducerServer**  
   - Default port: **8000**
2. **MasterServer**  
   - Default port: **65432**
3. **WorkerServers**  
   - Ports: **7000+** (or whatever your config uses)
   - Start multiple workers to simulate distribution

> If you have a `ClusterLauncher`, you can start the entire cluster from there.

### 2) Run Android App
- **Android Emulator:** set Master IP to `10.0.2.2`
- **Physical Device:** set Master IP to your computer’s LAN IP (e.g. `192.168.1.x`)

Make sure the ports match your backend configuration.

---

## Configuration
- Ports and worker count are typically configured in server constants/config files.
- If you change:
  - Master port
  - Reducer port
  - Worker ports
  - Worker mapping (hash / shards)
  
…make sure the Android client points to the correct Master host/port.

---

## Notes / Limitations
- This project uses raw **TCP sockets** (not HTTP).
- Running on different machines requires:
  - reachable IPs
  - open ports / firewall rules
- If your dataset is loaded from JSON files, ensure the file paths are correct.

---

## Screenshots 
- login
- customer search results
- filters
- manager dashboard

## Screenshots
![Login](screenshot/login.png)
![Search](screenshot/search.png)
![Cmd](screenshot/cmd.png)


