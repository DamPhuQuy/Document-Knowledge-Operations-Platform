# AWS Cloud & Edge Architecture Blueprint: Practical Design & Enterprise Trade-Offs
## Enterprise Document Knowledge & Operations Platform

> **Triết Lý Kiến Trúc Trọng Tâm (Core Architecture Philosophy):**
> *"Understand the Architecture & Master the Trade-Offs"* — Cấu hình chi phí **~$0.00 / tháng** là một bài tập rèn luyện ý thức tối ưu chi phí (Cost Awareness) và năng lực quản trị hệ thống (Hands-On Sysadmin), **KHÔNG PHẢI là mục tiêu tối thượng duy nhất** trong thiết kế kiến trúc đám mây.
>
> **Mục tiêu cốt lõi của tài liệu này là:**
> 1. **Hiểu rõ bản chất kiến trúc đám mây:** Nắm vững cách các dịch vụ cốt lõi (IAM, VPC, EC2, EBS, S3, CloudWatch, Cloudflare Edge) tương tác và vận hành dưới nắp capo.
> 2. **Phân tích sâu sắc sự đánh đổi (Architecture Trade-Offs):** Nhận thức rõ ràng *tại sao trong môi trường Production thực tế, doanh nghiệp sẵn sàng chi trả ngân sách lớn cho các dịch vụ Managed (AWS RDS, NAT Gateway, ALB, Multi-AZ)* để đổi lấy **Tính sẵn sàng cao (High Availability), Khả năng phục hồi thảm họa (Disaster Recovery / RPO-RTO), và Sự tinh gọn vận hành (Operational Simplicity)**.

---

## 1. Executive Summary & Hai Mô Hình Kiến Trúc Đối Sánh

Trong kỹ nghệ phần mềm doanh nghiệp, không có kiến trúc nào là "tốt nhất" một cách tuyệt đối, chỉ có kiến trúc **phù hợp nhất với bối cảnh và ràng buộc (Constraints)**. Tài liệu này đối sánh hai mô hình kiến trúc song song:

### 1.1. Mô Hình A: Cost-Aware Hands-On Lab / MVP ($0.00 / Tháng)
- **Bối cảnh áp dụng:** Giai đoạn R&D, Prototype, đồ án tốt nghiệp, bài lab kỹ thuật, hoặc môi trường thử nghiệm cá nhân.
- **Mục tiêu:** Tối ưu hóa tối đa trong hạn mức AWS Free Tier (12 Months & Always Free) kết hợp Cloudflare Always-Free Tier.
- **Ràng buộc chấp nhận (Trade-offs Accepted):** Chấp nhận tốn công sức quản trị thủ công (High Operational Overhead), chấp nhận điểm nghẽn đơn lẻ (Single Point of Failure - SPOF), và hiệu năng I/O bị giới hạn bởi bộ nhớ Swap trên đĩa SSD mạng.

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                   MÔ HÌNH A: COST-AWARE HANDS-ON LAB / MVP TOPOLOGY (~$0.00/MO)                 │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                      [ Client Browser ]                                          │
│                                              │                                                   │
│                                      (HTTPS / HTTP/3)                                            │
│                                              ▼                                                   │
│                        [ Cloudflare Modern Edge Network ]                                        │
│                        ├── Anycast DNS (< 15ms latency, Free vĩnh viễn)                          │
│                        ├── Universal SSL/TLS (Full Strict Mode, Auto Renewal)                    │
│                        ├── DDoS L3/L4/L7 & Web Application Firewall (WAF)                        │
│                        └── Edge Cache (Brotli Compression, HTTP/3, WebSockets)                   │
│                                              │                                                   │
│                        ┌─────────────────────┴─────────────────────┐                             │
│                        │ (Cách 1: Cloudflare Tunnel)               │ (Cách 2: Orange Cloud Proxy)│
│                        ▼ (Outbound TLS - 0 Inbound Port)           ▼ (Proxied A Record)          │
│        [ AWS EC2: Ubuntu 24.04 (t2/t3.micro - Free Tier 750h) ]                                  │
│        ├── Security Group: 0 Inbound Ports (hoặc chỉ nhận IP Cloudflare & SSH IP admin)          │
│        ├── 30GB gp3 EBS (Tự động cấp phát 3GB Swap Memory bù 1GB RAM)                           │
│        ├── Docker & Docker Compose v2 (Container Runtime)                                        │
│        └── Container Stack: Backend Spring Boot + FastAPI AI + PostgreSQL pgvector               │
│                                              │                                                   │
│                                       (IAM Instance Profile - No Secrets in Code)                │
│                                              ▼                                                   │
│                        [ AWS S3 Private Document Bucket ]                                        │
│                        (Server-Side Encryption AES256, 30-Day Lifecycle Rule)                    │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 1.2. Mô Hình B: Enterprise Resilient Production ($150 – $350+ / Tháng)
- **Bối cảnh áp dụng:** Môi trường Production thương mại phục vụ khách hàng thật, yêu cầu SLA 99.9%, dữ liệu tài chính/pháp lý không được phép mất mát.
- **Kiến trúc:** VPC 3-Tier chuẩn phân bổ trên tối thiểu **2 Availability Zones (Multi-AZ)**:
  - **Tier 1 (Public Subnets):** Application Load Balancer (ALB) nhận traffic HTTPS, phân bổ tải và tự động cách ly máy chủ lỗi. NAT Gateways đảm bảo đường truyền ra ngoài (Egress) cho cụm máy chủ nội bộ.
  - **Tier 2 (Private Application Subnets):** Auto Scaling Group (ASG) gồm nhiều EC2 instances (hoặc ECS Fargate) hoàn toàn **không có Public IP**, tự động co giãn theo tải CPU/Memory.
  - **Tier 3 (Private Isolated Database Subnets):** Amazon RDS PostgreSQL (Multi-AZ Deployment) với Standby Replica đồng bộ thời gian thực, tự động backup liên tục (Continuous WAL Archiving) lên S3.

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                    MÔ HÌNH B: ENTERPRISE MULTI-AZ PRODUCTION TOPOLOGY (RESILIENT)                │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                      [ Client Browser ]                                          │
│                                              │                                                   │
│                                      (HTTPS / HTTP/3)                                            │
│                                              ▼                                                   │
│                      [ Cloudflare Enterprise / AWS WAF + Route 53 ]                              │
│                                              │                                                   │
│                    ┌─────────────────────────┴─────────────────────────┐                         │
│                    ▼                                                   ▼                         │
│         [ Public Subnet - AZ-a ]                            [ Public Subnet - AZ-b ]             │
│         ├── AWS Application Load Balancer (ALB)             ├── AWS ALB (Standby / Cross-Zone)   │
│         └── Managed NAT Gateway AZ-a                        └── Managed NAT Gateway AZ-b         │
│                    │                                                   │                         │
│     ┌──────────────┴─────────────────────────┬─────────────────────────┴──────────────┐          │
│     ▼                                        ▼                                        ▼          │
│  [ Private App Subnet - AZ-a ]            [ Private App Subnet - AZ-b ]            [ Outbound ]  │
│  ├── EC2 / ECS App (Instance 1)           ├── EC2 / ECS App (Instance 2)           ├── NAT Egress│
│  └── Autoscaling Group (Scale out/in)     └── Autoscaling Group (Scale out/in)     └── Gemini API│
│     │                                        │                                                   │
│     └──────────────────────────────┬─────────┴────────────────────────────────┐                  │
│                                    ▼                                          ▼                  │
│                    [ Private DB Subnet - AZ-a ]               [ Private DB Subnet - AZ-b ]       │
│                    ├── AWS RDS PostgreSQL (Primary)           ├── AWS RDS PostgreSQL (Standby)   │
│                    └── Continuous WAL Archiving to S3         └── Synchronous Multi-AZ Failover  │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Bảng Ma Trận So Sánh Dịch Vụ: Lab MVP vs. Enterprise Production

| Tầng Hệ Thống | Mô Hình A: Hands-On Lab MVP ($0/mo) | Mô Hình B: Enterprise Production ($150–$350+/mo) | Lý Do Production Chấp Nhận Chi Trả |
| :--- | :--- | :--- | :--- |
| **Edge & Ingress** | Cloudflare Free DNS + Universal SSL | Cloudflare Pro/Ent + AWS WAF + ALB | Tích hợp WAF tùy biến cao, ACM SSL tự động, DDoS Layer 7 SLA. |
| **VPC & Network** | Default VPC, EC2 đặt ở Public Subnet | Custom VPC 3-Tier, Private Subnets + Multi-AZ NAT Gateways | **Bảo vệ tuyệt đối:** Máy chủ app & DB không có Public IP, tuân thủ SOC 2 / PCI-DSS. |
| **Compute Logic** | 1x EC2 `t2.micro` (1 vCPU, 1GB RAM) | Auto Scaling Group (2x `t4g.medium` hoặc ECS Fargate) | Tự động phục hồi khi phần cứng lỗi, triệt tiêu Single Point of Failure (SPOF). |
| **Database Tier** | Container PostgreSQL 16 chạy trên EC2 | Amazon RDS PostgreSQL Multi-AZ Deployment | **Zero Data Loss:** Backup liên tục down-to-the-second (PITR), tự động failover < 60s. |
| **Memory Strategy** | 1GB RAM + 3GB Swap file trên ổ đĩa SSD gp3 | 4GB – 8GB Physical DRAM mỗi node (No Swap) | **Tránh Swap Thrashing:** Đảm bảo độ trễ P99 < 100ms khi phục vụ hàng trăm users đồng thời. |
| **Search / Vector** | Extension `pgvector` co-location cùng DB | Dedicated OpenSearch Cluster / Pinecone Vector DB | **Workload Isolation:** Truy vấn vector nặng không làm nghẽn giao dịch cốt lõi. |
| **Object Storage** | S3 Standard (Ngưỡng 5GB Free Tier) | S3 Standard + Intelligent-Tiering + Multi-Region Replication | Tự động tối ưu chi phí khi dữ liệu lên hàng Terabytes, sao lưu chống thảm họa vùng. |
| **Vận Hành (Ops)** | Tự gõ lệnh SSH, tự backup script `cron` | AWS Managed Services (Automated Maintenance & Patching) | Tiết kiệm chi phí thuê kỹ sư DBA/DevOps trực đêm 24/7 (tiết kiệm hàng ngàn USD/tháng). |

---

## 3. Phân Tích Đánh Đổi Kiến Trúc Chuyên Sâu (Architecture Trade-Off Analysis)

Phần này phân tích chi tiết **5 quyết định kiến trúc then chốt**, giải thích lý do lựa chọn trong bài lab và lý do tại sao môi trường Production doanh nghiệp lại hành động hoàn toàn ngược lại.

---

### 3.1. Trade-Off 1: Network Ingress/Egress — Public Subnet + SG ($0) vs. Private Subnet + AWS NAT Gateway ($32+/Tháng)

#### 🔹 Thiết Kế Trong Bài Lab ($0 / Tháng):
- Đặt EC2 tại **Public Subnet** của Default VPC.
- Bật tường lửa **AWS Security Group** (chỉ cho phép SSH từ đúng địa chỉ IP máy admin, các cổng 80/443 chỉ cho phép traffic qua Cloudflare Proxy hoặc dùng Cloudflare Tunnel đóng 100% Inbound ports).
- **Mục tiêu học tập:** Nắm vững cơ chế Stateful Firewall của Security Group và cách cấu hình Cloudflare Zero Trust Tunnel.

#### 🔸 Thiết Kế Trong Production Doanh Nghiệp:
- Đặt toàn bộ máy chủ ứng dụng (Spring Boot, FastAPI) và Database vào **Private Subnets** hoàn toàn **KHÔNG có Public IPv4**.
- Mọi kết nối ra ngoài Internet (gọi Gemini API, tải bản vá OS, pull image) bắt buộc phải đi qua **AWS NAT Gateway** đặt tại Public Subnet. Chi phí cố định: ~$32.40/tháng/gateway + $0.045/GB dữ liệu. Với kiến trúc Multi-AZ (2 AZs), chi phí là ~$65/tháng.

#### 💡 Tại Sao Production Sẵn Sàng Trả Tiền Cho NAT Gateway?
1. **Phòng Thủ Đa Tầng (Defense-in-Depth) & Triệt Tiêu Bề Mặt Tấn Công:**
   - Khi máy chủ nằm trong Private Subnet, bảng định tuyến (Route Table) của nó **không có đường dẫn (route) tới Internet Gateway (IGW)**. Máy chủ hoàn toàn vô hình đối với mạng Internet toàn cầu.
   - Nếu máy chủ có Public IP (dù có Security Group che chắn), chỉ cần một sơ suất nhỏ của kỹ sư (như vô tình mở rule `0.0.0.0/0` khi debug), hoặc xuất hiện lỗ hổng Zero-day ở tầng Linux Kernel / SSH daemon, hacker có thể quét thấy IP và tấn công trực diện. Private Subnet triệt tiêu hoàn toàn nguy cơ này.
2. **Tuân Thủ Quy Chuẩn Doanh Nghiệp (Enterprise Compliance):**
   - Các chứng chỉ an ninh thông tin bắt buộc như **PCI-DSS** (xử lý thẻ thanh toán), **SOC 2 Type II**, **ISO 27001**, và **HIPAA** quy định rõ: *Hệ thống chứa dữ liệu người dùng và backend xử lý nghiệp vụ tuyệt đối không được gán Public IP trực tiếp*.
3. **Băng Thông Co Giãn Tự Động & Độ Tin Cậy Cấp Nhà Cung Cấp:**
   - Nhiều người nghĩ đến giải pháp "tự tạo 1 EC2 instance nhỏ làm NAT" để tiết kiệm tiền. Nhưng nếu EC2 NAT đó bị quá tải CPU hoặc crash, toàn bộ hệ sinh thái private của bạn sẽ mất kết nối Internet.
   - AWS NAT Gateway là dịch vụ fully-managed, tự động mở rộng băng thông lên đến **100 Gbps**, có cơ chế dự phòng phần cứng tích hợp sẵn và được AWS bảo đảm uptime 99.99%.

---

### 3.2. Trade-Off 2: Database Tier — Self-Hosted PostgreSQL on EC2 ($0) vs. Amazon RDS PostgreSQL ($50 – $250+/Tháng)

#### 🔹 Thiết Kế Trong Bài Lab ($0 / Tháng):
- Khởi chạy PostgreSQL 16 + extension `pgvector` dưới dạng một Docker container chạy chung trên máy chủ EC2, mount dữ liệu ra thư mục trên ổ cứng EBS gp3.
- **Mục tiêu học tập:** Hiểu rõ cơ chế volume mount của Docker, cấu hình tham số database (`shared_buffers`, `work_mem`, `max_connections`) trong điều kiện tài nguyên eo hẹp, và tự viết kịch bản `pg_dump`.

#### 🔸 Thiết Kế Trong Production Doanh Nghiệp:
- Sử dụng **Amazon RDS PostgreSQL (Multi-AZ Deployment)** hoặc **Amazon Aurora Serverless v2**.
- Chi phí: từ **$50 đến hơn $250/tháng** tùy cấu hình CPU/RAM và dung lượng lưu trữ.

#### 💡 Tại Sao Production Sẵn Sàng Trả Tiền Cho Amazon RDS?
1. **Khả Năng Phục Hồi Về Bất Kỳ Giây Nào Trong Quá Khứ (Point-in-Time Recovery - PITR):**
   - RDS tự động snapshot hàng ngày và liên tục tải các tệp nhật ký giao dịch (**Write-Ahead Logs - WAL**) lên Amazon S3 theo thời gian thực.
   - Nếu vào lúc `14:23:15` chiều một lập trình viên vô tình chạy nhầm lệnh `DROP TABLE customers` trên production, RDS cho phép bạn phục hồi toàn bộ database về đúng thời điểm **`14:23:14` (chỉ 1 giây trước khi lỗi xảy ra)**!
   - Với database tự dựng bằng Docker, nếu bạn chỉ cấu hình script `cron` chạy `pg_dump` vào 2h sáng mỗi ngày, khi sự cố xảy ra vào 17h chiều, doanh nghiệp của bạn **mất trắng toàn bộ 15 tiếng giao dịch của khách hàng** (RPO = 15 giờ). Đối với doanh nghiệp, mất dữ liệu đồng nghĩa với mất uy tín và mất tiền bạc không thể đo đếm.
2. **Sao Chép Đồng Bộ Đa Vùng & Tự Động Chuyển Vùng Khi Thảm Họa (Multi-AZ Synchronous Failover):**
   - RDS Multi-AZ tự động duy trì một máy chủ dự phòng (Standby Replica) đặt tại một Data Center vật lý hoàn toàn khác (Availability Zone khác). Mọi thao tác ghi dữ liệu đều được xác nhận đồng bộ (synchronous commit) trên cả 2 AZ.
   - Khi AZ chính gặp thảm họa (mất điện toàn vùng, cháy nổ, sự cố phần cứng máy chủ vật lý), RDS **tự động chuyển hướng kết nối DNS sang Standby Replica trong vòng 60 giây (RTO < 60s)** mà không cần kỹ sư phải thức dậy lúc nửa đêm để can thiệp.
   - Với PostgreSQL tự dựng trên 1 EC2, khi máy chủ EC2 hỏng phần cứng hoặc data center gặp sự cố, hệ thống của bạn sẽ **sập hoàn toàn (100% Downtime)** cho đến khi có người dựng lại từ đầu.
3. **Tự Động Vá Lỗ Hổng & Nâng Cấp Engine (Automated Maintenance & OS Patching):**
   - AWS tự động phát hiện và vá các lỗ hổng bảo mật nghiêm trọng của hệ điều hành và PostgreSQL engine trong khung giờ bảo trì (Maintenance Window) định sẵn mà không làm gián đoạn dịch vụ.
4. **Chi Phí Cơ Hội & Tinh Gọn Vận Hành (Operational Simplicity & Total Cost of Ownership - TCO):**
   - Để tự vận hành một cụm PostgreSQL đạt chuẩn enterprise (high availability, automated failover, PITR backup, monitoring, vacuum tuning), doanh nghiệp bắt buộc phải thuê ít nhất một **Database Administrator (DBA)** hoặc Senior DevOps với mức lương từ **$2,000 – $5,000/tháng**.
   - Chi trả $150 – $200/tháng cho AWS RDS để đổi lấy sự an tâm tuyệt đối và giải phóng kỹ sư tập trung vào phát triển tính năng sản phẩm là một **khoản đầu tư siêu lợi nhuận**.

---

### 3.3. Trade-Off 3: Traffic Routing & High Availability — Single EC2 Nginx ($0) vs. Application Load Balancer + Auto Scaling ($20 – $80+/Tháng)

#### 🔹 Thiết Kế Trong Bài Lab ($0 / Tháng):
- Tận dụng Cloudflare DNS trỏ thẳng về IP của 1 máy chủ EC2 duy nhất. Bên trong EC2, dùng Nginx làm reverse proxy điều phối traffic tới container React và Spring Boot.
- **Mục tiêu học tập:** Hiểu nguyên lý hoạt động của Web Server, Reverse Proxy Nginx, cấu hình upstream và cơ chế Proxy của Cloudflare.

#### 🔸 Thiết Kế Trong Production Doanh Nghiệp:
- Sử dụng **AWS Application Load Balancer (ALB)** phân bổ traffic cho một nhóm máy chủ **Auto Scaling Group (ASG)** nằm trên tối thiểu 2 Availability Zones.
- Chi phí cố định của ALB: ~$16.20 – $22.00/tháng + phí LCU theo lưu lượng.

#### 💡 Tại Sao Production Sẵn Sàng Trả Tiền Cho ALB + ASG?
1. **Triệt Tiêu Điểm Nghẽn Đơn Lẻ (Eliminate Single Point of Failure - SPOF):**
   - Một máy chủ EC2 đơn lẻ dù tốt đến đâu cũng có thể bị chết do AWS bảo trì phần cứng vật lý bên dưới (Hardware Retirement), lỗi kernel panic, hoặc ứng dụng bị memory leak làm sập OS.
   - Khi dùng ALB kết hợp ASG, nếu một instance gặp sự cố, ALB sẽ tự động ngắt định tuyến tới instance đó trong vài giây thông qua cơ chế **Health Check Probe**, đồng thời ASG tự động tiêu hủy instance hỏng và khởi tạo một instance mới thay thế (Self-Healing Architecture).
2. **Triển Khai Cập Nhật Không Gián Đoạn (Zero-Downtime Deployment):**
   - Với kiến trúc 1 EC2, mỗi lần bạn deploy bản build mới của Spring Boot hoặc restart container, người dùng đang truy cập sẽ bị gián đoạn (lỗi `502 Bad Gateway` trong vài chục giây).
   - ALB hỗ trợ các kỹ thuật triển khai hiện đại: **Rolling Update, Blue-Green Deployment, và Canary Deployment**. Phiên bản mới được khởi động và kiểm tra sức khỏe hoàn tất trên instance mới trước khi ALB bắt đầu chuyển lưu lượng sang, đảm bảo người dùng không nhận thấy bất kỳ sự gián đoạn nào.
3. **Tích Hợp Sâu Hệ Sinh Thái Bảo Mật Của AWS:**
   - ALB tích hợp trực tiếp với **AWS WAF** (ngăn chặn SQL Injection, Cross-Site Scripting, Rate Limiting ở tầng ứng dụng), tích hợp **AWS Certificate Manager (ACM)** cấp phát và gia hạn chứng chỉ SSL hoàn toàn miễn phí trong nội bộ AWS.

---

### 3.4. Trade-Off 4: Quản Trị Bộ Nhớ — 3GB Swap trên Ổ Cứng gp3 ($0) vs. Nâng Cấp RAM Vật Lý ($15 – $50+/Tháng)

#### 🔹 Thiết Kế Trong Bài Lab ($0 / Tháng):
- Tận dụng hạn mức 30GB SSD gp3 miễn phí của Free Tier để tạo **3GB Swap Memory**, giúp nâng tổng bộ nhớ ảo của máy chủ `t2.micro` từ 1GB RAM lên 4GB khả dụng.
- Tinh chỉnh `vm.swappiness=10` và cấu hình Serial GC (`-XX:+UseSerialGC`) cho JVM Spring Boot.
- **Mục tiêu học tập:** Rèn luyện kỹ năng Sysadmin thực chiến ở mức sâu nhất: hiểu cơ chế Linux Virtual Memory, Paging, Swapping, OOM-killer và cách tối ưu hóa footprint của Java Virtual Machine.

#### 🔸 Thiết Kế Trong Production Doanh Nghiệp:
- Nâng cấp instance type lên dòng máy chủ chuẩn sản xuất: `t4g.medium` (2 vCPU, 4GB RAM - kiến trúc Graviton ARM giá rẻ hiệu năng cao) hoặc `m6i.large` (2 vCPU, 8GB RAM).
- Không cấu hình Swap hoặc chỉ để 512MB Swap mang tính chất phòng vệ khẩn cấp.

#### 💡 Tại Sao Production Bắt Buộc Phải Dùng RAM Vật Lý Thật?
1. **Bản Chất Tốc Độ: Khoảng Cách Giữa Nano-Giây và Milli-Giây:**
   - Tốc độ đọc/ghi của **RAM vật lý (DDR4/DDR5)** đo bằng **nano-giây (~10 – 100 ns)** với băng thông lên tới hàng chục GB/s.
   - Tốc độ đọc/ghi của **Swap file trên ổ đĩa SSD mạng EBS gp3** đo bằng **milli-giây (~1 – 5 ms)** — **CHẬM HƠN TỪ 10,000 ĐẾN 50,000 LẦN!**
2. **Hiểm Họa "Swap Thrashing" Dưới Tải Người Dùng Thực:**
   - Trong bài lab, khi chỉ có 1-2 người thử nghiệm, Swap hoạt động rất tốt vì các trang nhớ nhàn rỗi (idle pages) được đẩy sang đĩa và nằm yên ở đó.
   - Nhưng khi đưa vào Production với hàng chục hoặc hàng trăm request đồng thời, CPU liên tục phải tráo đổi dữ liệu giữa RAM và ổ cứng (Paging In / Paging Out liên tục). Hiện tượng này gọi là **Swap Thrashing**.
   - Hậu quả: Chỉ số CPU I/O Wait chạm mốc 100%, thời gian đáp ứng API (P99 latency) nhảy vọt từ **80ms lên 10 – 20 giây**, và toàn bộ hệ thống bị đóng băng hoàn toàn.
3. **Cam Kết Chất Lượng Dịch Vụ (SLA):**
   - Khách hàng doanh nghiệp không chấp nhận hệ thống phản hồi chậm chạp. Đầu tư thêm $15 – $30/tháng để có 4GB – 8GB RAM thực tế là điều kiện tiên quyết để đảm bảo trải nghiệm người dùng mượt mà và ổn định.

---

### 3.5. Trade-Off 5: Tìm Kiếm & Vector Embeddings — PostgreSQL pgvector Co-Location ($0) vs. Dedicated Search Cluster ($50 – $150+/Tháng)

#### 🔹 Thiết Kế Trong Bài Lab ($0 / Tháng):
- Cài đặt extension `pgvector` trực tiếp trong database PostgreSQL duy nhất của hệ thống. Dữ liệu quan hệ nghiệp vụ (Users, Documents, Audits) và vector embeddings nằm chung trên một cụm dữ liệu.
- **Mục tiêu học tập:** Tiếp cận công nghệ Hybrid Search hiện đại, hiểu thuật toán chỉ mục HNSW/IVFFlat, đơn giản hóa kiến trúc kết nối và không phát sinh thêm bất kỳ chi phí phần cứng nào.

#### 🔸 Thiết Kế Trong Production Doanh Nghiệp:
- Tách riêng chức năng tìm kiếm ngữ nghĩa sang **Amazon OpenSearch Service** hoặc các dịch vụ vector chuyên dụng như **Pinecone, Qdrant Cloud, Milvus Cluster**.

#### 💡 Tại Sao Production Chấp Nhận Tách Riêng Vector Engine?
1. **Cách Ly Tải Tính Toán (Workload Isolation):**
   - Các phép toán đo khoảng cách vector (Cosine Similarity, Euclidean Distance) trên không gian nhiều chiều (ví dụ: vector 1536 chiều của OpenAI hay 768 chiều của Gemini) đòi hỏi **năng lực tính toán CPU và RAM khổng lồ**.
   - Nếu đặt chung, khi người dùng thực hiện một truy vấn tìm kiếm RAG phức tạp trên hàng ngàn tài liệu, tác vụ tính toán vector có thể chiếm sạch 100% CPU và khóa buffer cache của PostgreSQL. Kết quả là các nghiệp vụ giao dịch cốt lõi (OLTP) như đăng nhập, ghi log phê duyệt hay cập nhật trạng thái văn bản sẽ bị treo hoặc timeout theo.
2. **Khả Năng Co Giãn Độc Lập (Independent Scalability):**
   - Lưu trữ văn bản nghiệp vụ tăng trưởng theo cấp số cộng, nhưng dữ liệu vector chunks tăng trưởng theo cấp số nhân (1 văn bản có thể sinh ra hàng trăm đoạn chunks). Tách rời dịch vụ cho phép scale-up hoặc scale-out cụm tìm kiếm mà không cần nâng cấp toàn bộ database quan hệ đắt đỏ.

---


## 4. Hướng Dẫn Thực Chiến Hands-On 8 Bước (A-Z)

---

### BƯỚC 1: IAM & Quản Trị Chi Phí (Cost Governance & Security)
*Mục tiêu học:* Hiểu cách AWS tính tiền, tạo chốt chặn tự động và nguyên tắc bảo mật đặc quyền tối thiểu (**Principle of Least Privilege**).

#### 1.1. Kích hoạt AWS Budgets (Chốt chặn bảo vệ)
1. Đăng nhập AWS Console $\rightarrow$ Tìm dịch vụ **Billing and Cost Management** $\rightarrow$ chọn **Budgets**.
2. Bấm **Create budget** $\rightarrow$ Chọn **Zero spend budget** (hoặc đặt ngưỡng $1.00/tháng).
3. Điền Email cá nhân nhận thông báo.
   - 👉 *Bài học:* Bất kể khi nào tài khoản phát sinh dù chỉ **$0.01**, AWS sẽ gửi email cảnh báo ngay lập tức.

#### 1.2. Tạo IAM Role cho EC2 (Không bao giờ lưu Access Key vào code)
1. Vào dịch vụ **IAM** $\rightarrow$ **Roles** $\rightarrow$ Bấm **Create role**.
2. **Trusted entity type**: Chọn **AWS service** $\rightarrow$ Use case: **EC2**.
3. **Permissions**: Bấm **Create policy** (chọn tab JSON), dán quyền tối thiểu cho S3:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": ["s3:ListBucket", "s3:GetBucketLocation"],
         "Resource": "arn:aws:s3:::docknowledge-*"
       },
       {
         "Effect": "Allow",
         "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
         "Resource": "arn:aws:s3:::docknowledge-*/*"
       }
     ]
   }
   ```
4. Đặt tên Policy: `DocKnowledgeS3AccessPolicy` $\rightarrow$ Đặt tên Role: `DocKnowledgeEC2Role` $\rightarrow$ Bấm **Create role**.
   - 👉 *Bài học:* Khi gán Role này cho EC2, thư viện AWS SDK trong Spring Boot sẽ tự động lấy token xác thực tạm thời qua Instance Metadata Service (IMDSv2) mà không cần cấu hình hardcoded Access Key trong file `.env`!

---

### BƯỚC 2: VPC & Mạng Đám Mây (Networking & Security Groups)
*Mục tiêu học:* Hiểu cách một máy chủ kết nối ra Internet và cách tường lửa tầng mạng hoạt động.

1. AWS đã tạo sẵn một **Default VPC** có dải IP `172.31.0.0/16` kèm Internet Gateway và Public Subnet.
2. Vào **VPC** $\rightarrow$ **Security Groups** $\rightarrow$ Bấm **Create security group**:
   - **Name**: `docknowledge-web-sg`
   - **VPC**: Chọn Default VPC.
   - **Inbound rules** (Tường lửa chiều vào):
     - Rule 1: Type `SSH`, Port `22`, Source: `My IP` (Chỉ cho phép IP mạng nhà bạn SSH vào, bảo vệ khỏi hacker scan port).
     - Rule 2: Type `HTTP`, Port `80`, Source: `0.0.0.0/0` (Nhận traffic web qua Cloudflare).
     - Rule 3: Type `HTTPS`, Port `443`, Source: `0.0.0.0/0` (Nhận traffic SSL qua Cloudflare).
   - **Outbound rules**: Mặc định cho phép `All traffic` (để server tải Docker images và gọi External LLM APIs).
   - 👉 *Bài học:* Security Group là **Stateful Firewall** — khi cho phép Inbound, kết nối trả về (Outbound) sẽ tự động được chấp nhận mà không cần mở thêm cổng.

---

### BƯỚC 3: Amazon S3 (Lưu Trữ Tệp Đối Tượng Nhị Phân)
*Mục tiêu học:* Hiểu Object Storage, cơ chế bảo vệ dữ liệu và vòng đời lưu trữ (Lifecycle Rules).

1. Vào dịch vụ **S3** $\rightarrow$ Bấm **Create bucket**:
   - **Bucket name**: `docknowledge-docs-<ten-ban>-sg` (Tên S3 phải là duy nhất toàn cầu).
   - **AWS Region**: Chọn `ap-southeast-1` (Singapore).
   - **Object Ownership**: Chọn `ACLs disabled (recommended)`.
   - **Block Public Access**: Giữ nguyên dấu tích **Block all public access** (Bảo mật tuyệt đối, ngăn tải trộm file từ Internet).
   - **Bucket Versioning**: Bấm **Enable** (đáp ứng nghiệp vụ quản lý phiên bản tài liệu).
   - **Default encryption**: `Server-side encryption with Amazon S3 managed keys (SSE-S3)`.
2. **Thiết lập Lifecycle Rule (Giữ dung lượng luôn dưới 5GB Free Tier)**:
   - Vào tab **Management** trong Bucket vừa tạo $\rightarrow$ Bấm **Create lifecycle rule**.
   - Rule name: `DeleteOldVersionsAfter30Days`.
   - Chọn: `Permanently delete noncurrent versions of objects`.
   - Số ngày: `30` ngày.
   - 👉 *Bài học:* Rule này tự động dọn sạch các version file cũ sau 30 ngày, bảo đảm không bao giờ vượt quá 5GB miễn phí.

---

### BƯỚC 4: EC2 & Quản Trị Hệ Thống Linux (Compute & Sysadmin)
*Mục tiêu học:* Khởi tạo máy chủ đám mây, gán IAM Role, cấp phát 3GB Swap Memory để cứu nguy 1GB RAM và cài Docker Engine.

#### 4.1. Khởi tạo EC2 Instance
1. Vào dịch vụ **EC2** $\rightarrow$ Bấm **Launch instances**:
   - **Name**: `docknowledge-server`
   - **AMI**: `Ubuntu Server 24.04 LTS` (Eligible for Free Tier).
   - **Instance type**: `t2.micro` (hoặc `t3.micro`).
   - **Key pair**: Tạo key pair mới `my-ec2-key.pem` và tải về máy.
   - **Network settings**:
     - Auto-assign public IP: `Enable`.
     - Select existing security group: Chọn `docknowledge-web-sg` đã tạo ở Bước 2.
   - **Configure storage**: Đổi dung lượng thành **`30 GiB`** loại **`gp3`** (Mức tối đa miễn phí của Free Tier).
   - **Advanced details**:
     - **IAM instance profile**: Chọn `DocKnowledgeEC2Role` đã tạo ở Bước 1.
2. Bấm **Launch instance**.

#### 4.2. SSH vào máy chủ và cấu hình 3GB Swap Memory
Mở terminal trên máy bạn (tại thư mục chứa file `my-ec2-key.pem`):
```bash
chmod 400 my-ec2-key.pem
ssh -i my-ec2-key.pem ubuntu@<EC2_PUBLIC_IP>
```

Khi đã đăng nhập vào EC2, chạy các lệnh sau để tạo **3GB Swap Memory** trên ổ đĩa SSD gp3:
```bash
# Tạo file swap 3GB
sudo fallocate -l 3G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Đăng ký tự động mount khi reboot
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Tối ưu độ nhạy swappiness (chỉ swap khi RAM thực sự chạm đỉnh)
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf

# Kiểm tra: RAM 1GB + Swap 3GB = 4GB bộ nhớ khả dụng!
free -h
```

#### 4.3. Cài đặt Docker & Docker Compose v2
```bash
# Cài đặt Docker Engine chính thức từ Docker repository
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Cho phép user ubuntu thao tác docker không cần sudo
sudo usermod -aG docker ubuntu
newgrp docker
```

---

### BƯỚC 5: Tinh Chỉnh Bộ Nhớ Cho Các Dịch Vụ Container

Để toàn bộ cụm dịch vụ chạy ổn định trong giới hạn tài nguyên:

1. **JVM Flag cho Spring Boot:** Đặt biến môi trường `JAVA_TOOL_OPTIONS`:
   ```bash
   JAVA_TOOL_OPTIONS="-Xms128m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError"
   ```
   *Hiệu quả:* Giới hạn heap tối đa 256MB, sử dụng Serial GC siêu nhẹ, đảm bảo Spring Boot vận hành ổn định trong ~300MB RAM.

2. **Cấu hình PostgreSQL 16 & pgvector:** Trong `docker-compose.yaml`:
   ```yaml
   postgres:
     image: postgres:16-alpine
     command: >
       postgres -c shared_buffers=128MB
                -c work_mem=4MB
                -c maintenance_work_mem=64MB
                -c max_connections=30
   ```

3. **FastAPI AI Worker:** Chạy Uvicorn ở chế độ đơn luồng (`--workers 1`), chiếm không quá 90MB – 120MB RAM.

---

### BƯỚC 6: Triển Khai Nền Tảng Bằng Docker Compose

```bash
# Clone source code
git clone https://github.com/<your-org>/Document-Knowledge-Operations-Platform.git
cd Document-Knowledge-Operations-Platform

# Cấu hình biến môi trường production
cp .env.prod.example .env
nano .env
```
Điền các giá trị thực tế:
- `S3_BUCKET`: Tên S3 Bucket đã tạo ở Bước 3.
- `S3_REGION`: `ap-southeast-1`
- `SPRING_PROFILES_ACTIVE`: `prod`

Khởi chạy hệ thống:
```bash
docker compose up -d
docker compose ps
```

---

### BƯỚC 7: Tích Hợp Cloudflare (Edge DNS, Universal SSL & Chống DDoS)
*Mục tiêu học:* Sử dụng Cloudflare Edge Network làm lá chắn phía trước AWS EC2.

1. **Đổi Nameservers sang Cloudflare**:
   - Đăng ký tài khoản miễn phí tại [Cloudflare](https://dash.cloudflare.com/).
   - Bấm **Add a Site** $\rightarrow$ Nhập tên miền của bạn (ví dụ: `yourdomain.com`) $\rightarrow$ Chọn **Free Plan ($0)**.
   - Thay đổi 2 địa chỉ Nameservers tại nhà cung cấp tên miền của bạn sang Nameservers của Cloudflare.
2. **Cấu hình bản ghi DNS**:
   - Vào menu **DNS** $\rightarrow$ **Records** trên Cloudflare Dashboard.
   - Thêm bản ghi:
     - Type: `A`
     - Name: `app` (kết quả truy cập: `https://app.yourdomain.com`)
     - IPv4 address: Điền **Public IP** của máy chủ EC2.
     - **Proxy status**: BẬT **Proxied (Đám mây màu cam)**.
   - 👉 *Bài học:* Khi bật đám mây cam, toàn bộ người dùng kết nối qua mạng biên của Cloudflare. IP thật của máy chủ EC2 được giấu hoàn toàn, hacker không thể tấn công trực tiếp.
3. **Bật mã hóa SSL Full (Strict) & Nén Dữ Liệu**:
   - Vào menu **SSL/TLS** $\rightarrow$ Chọn chế độ **Full** (hoặc **Full (Strict)**).
   - Vào menu **Edge Certificates** $\rightarrow$ Bật **Always Use HTTPS**, bật **HTTP/3**, bật **Brotli**.
   - 👉 *Bài học:* Trang web của bạn có chứng chỉ SSL xanh hợp lệ 100% trên toàn cầu, tự động gia hạn vĩnh viễn mà không tốn chi phí quản lý hay mua chứng chỉ SSL.

---

### BƯỚC 8: Giám Sát Sức Khỏe Bằng AWS CloudWatch
*Mục tiêu học:* Hiểu về số liệu giám sát đám mây (Cloud Metrics) và cảnh báo ngưỡng (Threshold Alarms).

1. Vào dịch vụ **CloudWatch** $\rightarrow$ **Alarms** $\rightarrow$ **Create alarm**.
2. **Select metric**: Chọn `EC2` $\rightarrow$ `Per-Instance Metrics` $\rightarrow$ Tìm instance `docknowledge-server` $\rightarrow$ Chọn chỉ số `CPUUtilization`.
3. **Conditions**: Cảnh báo khi `CPUUtilization >= 85%` liên tục trong 5 phút.
4. **Notification**: Chọn tạo mới một **SNS Topic** và điền email của bạn để nhận email cảnh báo khi server bị quá tải.

---

## 5. Tự Động Hóa Triển Khai Bằng Terraform (Infrastructure as Code - IaC)

Sau khi đã thực hành từng bước trên AWS Management Console để hiểu rõ bản chất của từng dịch vụ đám mây, bước tiếp theo của một **Technical Lead & Cloud Architect** là **mã hóa toàn bộ hạ tầng thành code (Infrastructure as Code)**.

### 5.1. Tại Sao Cần Terraform Dù Đã Thao Tác Bằng Tay?
1. **Khả năng tái lập 100% (Reproducibility):** Nếu máy chủ gặp sự cố hoặc cần dựng môi trường thử nghiệm mới, chỉ cần chạy 1 lệnh thay vì tốn 2 tiếng click console.
2. **Loại bỏ sai sót do con người (Human Error Elimination):** Tránh quên bật mã hóa S3, quên cấp phát Swap, hoặc cấu hình sai Inbound port mở toang ra Internet.
3. **VŨ KHÍ BẢO VỆ VÍ TIỀN TUYỆT ĐỐI (`terraform destroy`):** Rủi ro lớn nhất khi học AWS là **quên xóa tài nguyên sau buổi bảo vệ đồ án/demo**, dẫn đến bị trừ tiền bất ngờ sau khi hết hạn 12 tháng Free Tier. Với Terraform, chỉ cần chạy đúng 1 câu lệnh `terraform destroy`, toàn bộ máy chủ, ổ cứng, bucket, security group sẽ bị thu hồi sạch sẽ trong 90 giây!

---

### 5.2. Cấu Trúc Thư Mục Terraform Enterprise Chuẩn

Khi sẵn sàng tự động hóa, cấu trúc thư mục triển khai được tổ chức như sau:

```text
infra/terraform/
├── providers.tf             # Khai báo AWS & Cloudflare providers
├── variables.tf             # Biến đầu vào với validation và giá trị mặc định
├── terraform.tfvars.example # File mẫu cấu hình secrets & biến môi trường
├── iam.tf                   # IAM Role, Policy & Instance Profile (Principle of Least Privilege)
├── s3.tf                    # S3 Bucket, AES256 Encryption, Versioning, 30-Day Lifecycle
├── security_groups.tf       # Stateful Firewall (Chỉ mở SSH admin & Web traffic)
├── ec2.tf                   # Data source AMI Ubuntu 24.04, t2.micro, 30GB gp3, Cloud-init User Data
├── cloudflare.tf            # DNS A Record (Proxied), SSL Strict Mode, HTTP/3, Brotli
└── outputs.tf               # Xuất Public IP, S3 Bucket Name, HTTPS Application URL
```

---

### 5.3. Chi Tiết Mã Nguồn Khai Báo Hạ Tầng (HCL)

#### 1. `providers.tf` — Khai Báo Nhà Cung Cấp Dịch Vụ
```hcl
terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Document-Knowledge-Operations-Platform"
      Environment = var.environment
      CostCenter  = "Zero-Cost-Free-Tier"
      ManagedBy   = "Terraform"
    }
  }
}

provider "cloudflare" {
  api_token = var.cloudflare_api_token
}
```

#### 2. `variables.tf` — Tham Số Hóa Đầu Vào
```hcl
variable "aws_region" {
  description = "AWS Region triển khai hạ tầng (Singapore để có độ trễ thấp nhất về VN)"
  type        = string
  default     = "ap-southeast-1"
}

variable "environment" {
  description = "Tên môi trường (prod, staging, dev)"
  type        = string
  default     = "prod"
}

variable "instance_type" {
  description = "Loại máy chủ EC2 Free Tier (t2.micro hoặc t3.micro)"
  type        = string
  default     = "t2.micro"
}

variable "ebs_volume_size" {
  description = "Dung lượng ổ đĩa SSD gp3 (tối đa 30GB theo hạn mức Free Tier)"
  type        = number
  default     = 30
}

variable "key_name" {
  description = "Tên SSH Key Pair đã tạo sẵn trên AWS Console"
  type        = string
}

variable "admin_ssh_cidr" {
  description = "Địa chỉ IP tĩnh của quản trị viên được phép SSH vào EC2 (ví dụ: 14.161.x.x/32)"
  type        = string
}

variable "cloudflare_api_token" {
  description = "Cloudflare API Token có quyền sửa Zone DNS và SSL Settings"
  type        = string
  sensitive   = true
}

variable "cloudflare_zone_id" {
  description = "Zone ID của domain trên Cloudflare Dashboard"
  type        = string
}

variable "domain_name" {
  description = "Tên miền chính (ví dụ: yourdomain.com)"
  type        = string
}

variable "subdomain" {
  description = "Tên miền phụ phục vụ ứng dụng (ví dụ: app)"
  type        = string
  default     = "app"
}
```

#### 3. `terraform.tfvars.example` — Mẫu Cấu Hình Thực Tế
```hcl
aws_region           = "ap-southeast-1"
environment          = "prod"
instance_type        = "t2.micro"
ebs_volume_size      = 30
key_name             = "my-ec2-key"
admin_ssh_cidr       = "14.161.20.55/32" # Thay bằng IP máy bạn: curl ifconfig.me
cloudflare_api_token = "cf_api_token_here_xxxxxxxxx"
cloudflare_zone_id   = "cf_zone_id_here_xxxxxxxxx"
domain_name          = "yourdomain.com"
subdomain            = "app"
```

#### 4. `iam.tf` — Quản Trị Danh Tính & Phân Quyền (Principle of Least Privilege)
```hcl
# IAM Role gắn cho EC2 Instance
resource "aws_iam_role" "ec2_s3_role" {
  name = "docknowledge-ec2-s3-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# Policy chỉ cho phép thao tác với bucket tài liệu cụ thể (Không dùng full s3:*)
resource "aws_iam_policy" "s3_access_policy" {
  name        = "docknowledge-s3-access-policy"
  description = "Chỉ cho phép đọc/ghi/xóa object trong bucket dự án"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:ListBucket",
          "s3:GetBucketLocation"
        ]
        Resource = aws_s3_bucket.document_storage.arn
      },
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ]
        Resource = "${aws_s3_bucket.document_storage.arn}/*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "attach_s3" {
  role       = aws_iam_role.ec2_s3_role.name
  policy_arn = aws_iam_policy.s3_access_policy.arn
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "docknowledge-ec2-instance-profile"
  role = aws_iam_role.ec2_s3_role.name
}
```

#### 5. `s3.tf` — Lưu Trữ Tệp Đối Tượng & Tối Ưu Hóa Dung Lượng Free Tier
```hcl
# Sinh hậu tố ngẫu nhiên để đảm bảo tên bucket duy nhất trên toàn cầu
resource "random_string" "bucket_suffix" {
  length  = 8
  special = false
  upper   = false
}

resource "aws_s3_bucket" "document_storage" {
  bucket        = "docknowledge-docs-${random_string.bucket_suffix.result}"
  force_destroy = false
}

# Khóa 100% truy cập công khai từ bên ngoài
resource "aws_s3_bucket_public_access_block" "block_public" {
  bucket = aws_s3_bucket.document_storage.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Bật mã hóa SSE-S3 mặc định (AES256) không tốn thêm chi phí
resource "aws_s3_bucket_server_side_encryption_configuration" "s3_encryption" {
  bucket = aws_s3_bucket.document_storage.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Bật lưu vết lịch sử phiên bản file (Versioning)
resource "aws_s3_bucket_versioning" "s3_versioning" {
  bucket = aws_s3_bucket.document_storage.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Lifecycle Rule: Tự động xóa version cũ sau 30 ngày để luôn ở dưới 5GB Free Tier
resource "aws_s3_bucket_lifecycle_configuration" "s3_lifecycle" {
  bucket = aws_s3_bucket.document_storage.id

  rule {
    id     = "delete-old-versions-after-30-days"
    status = "Enabled"

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}
```

#### 6. `security_groups.tf` — Tường Lửa Tầng Mạng (Stateful Firewall)
```hcl
resource "aws_security_group" "web_sg" {
  name        = "docknowledge-web-sg"
  description = "Tường lửa kiểm soát truy cập vào EC2 t2/t3.micro"
  vpc_id      = data.aws_vpc.default.id

  # Cổng 22 SSH: Chỉ mở cho IP cá nhân của Admin
  ingress {
    description = "SSH access strictly from admin workstation"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_ssh_cidr]
  }

  # Cổng 80 HTTP: Nhận lưu lượng Web từ Cloudflare
  ingress {
    description = "HTTP web traffic via Cloudflare Proxy"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Cổng 443 HTTPS: Nhận lưu lượng mã hóa từ Cloudflare
  ingress {
    description = "HTTPS web traffic via Cloudflare Proxy"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Outbound: Cho phép kết nối ra ngoài để cập nhật hệ điều hành, kéo Docker image & gọi Gemini API
  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "docknowledge-web-sg"
  }
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}
```

#### 7. `ec2.tf` — Khởi Tạo Máy Chủ & Tự Động Hóa Cài Đặt Qua Cloud-Init
```hcl
# Tự động truy vấn phiên bản AMI Ubuntu 24.04 LTS chính thức từ Canonical
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "server" {
  ami                  = data.aws_ami.ubuntu.id
  instance_type        = var.instance_type
  key_name             = var.key_name
  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name

  vpc_security_group_ids      = [aws_security_group.web_sg.id]
  subnet_id                   = tolist(data.aws_subnets.default.ids)[0]
  associate_public_ip_address = true

  # Cấu hình 30GB SSD gp3 theo hạn mức tối đa của AWS Free Tier
  root_block_device {
    volume_size           = var.ebs_volume_size
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }

  # Kịch bản Cloud-init: Tự động chạy khi máy chủ khởi động lần đầu
  user_data = <<-EOF
    #!/bin/bash
    set -ex

    # 1. Cấp phát 3GB Swap Memory trên ổ đĩa gp3 để mở rộng bộ nhớ khả dụng lên 4GB
    fallocate -l 3G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab

    # Tinh chỉnh độ nhạy swappiness để ưu tiên RAM vật lý
    sysctl vm.swappiness=10
    echo 'vm.swappiness=10' >> /etc/sysctl.conf

    # 2. Cài đặt Docker Engine và Docker Compose v2 chính thức
    apt-get update -y
    apt-get install -y ca-certificates curl gnupg
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg

    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Phân quyền chạy Docker cho user ubuntu mà không cần gõ sudo
    usermod -aG docker ubuntu
  EOF

  tags = {
    Name = "docknowledge-server"
  }
}
```

#### 8. `cloudflare.tf` — Mạng Lưới Biên Cloudflare, DNS & Bảo Vệ SSL
```hcl
# Tạo bản ghi DNS loại A trỏ thẳng vào IP Public của EC2, bật Đám mây cam (Proxy)
resource "cloudflare_record" "app" {
  zone_id = var.cloudflare_zone_id
  name    = var.subdomain
  value   = aws_instance.server.public_ip
  type    = "A"
  ttl     = 1 # Automatic TTL
  proxied = true
}

# Kích hoạt toàn bộ tính năng tăng tốc & bảo mật miễn phí của Cloudflare
resource "cloudflare_zone_settings_override" "zone_settings" {
  zone_id = var.cloudflare_zone_id

  settings {
    ssl              = "strict"
    always_use_https = "on"
    http3            = "on"
    brotli           = "on"
    min_tls_version  = "1.2"
    security_header {
      enabled = true
      max_age = 31536000
    }
  }
}
```

#### 9. `outputs.tf` — Kết Quả Triển Khai
```hcl
output "ec2_public_ip" {
  description = "Địa chỉ Public IPv4 của máy chủ EC2"
  value       = aws_instance.server.public_ip
}

output "s3_bucket_name" {
  description = "Tên bucket Amazon S3 vừa được tạo"
  value       = aws_s3_bucket.document_storage.bucket
}

output "application_url" {
  description = "Địa chỉ truy cập nền tảng được bảo vệ qua Cloudflare Edge"
  value       = "https://${var.subdomain}.${var.domain_name}"
}

output "ssh_command" {
  description = "Lệnh SSH trực tiếp vào máy chủ quản trị"
  value       = "ssh -i ${var.key_name}.pem ubuntu@${aws_instance.server.public_ip}"
}
```

---

### 5.4. Quy Trình Vận Hành Với Terraform CLI

```bash
# Bước 1: Khởi tạo thư mục và tải các provider plugin
cd infra/terraform
terraform init

# Bước 2: Tạo file biến môi trường từ mẫu
cp terraform.tfvars.example terraform.tfvars
nano terraform.tfvars # Điền key_name, admin_ssh_cidr, cloudflare_api_token, v.v.

# Bước 3: Xem trước kế hoạch triển khai (Không làm thay đổi hạ tầng)
terraform plan -out=tfplan

# Bước 4: Thực thi triển khai hạ tầng tự động (Khoảng 2-3 phút)
terraform apply tfplan

# Bước 5: Kiểm tra kết quả outputs
terraform output
```

> [!IMPORTANT]
> **KHI BẢO VỆ ĐỒ ÁN XONG HOẶC KHÔNG CÒN SỬ DỤNG:**
> Hãy chạy câu lệnh sau để xóa sạch 100% tài nguyên, đảm bảo an toàn tuyệt đối cho tài khoản thẻ ngân hàng:
> ```bash
> terraform destroy
> ```
> *(Gõ `yes` khi được hỏi để xác nhận thu hồi tài nguyên)*

---

## 6. Ma Trận So Sánh: Hands-On Console vs Terraform IaC

| Tiêu Chí So Sánh | Phương Pháp 1: Hands-On Console | Phương Pháp 2: Terraform IaC |
| :--- | :--- | :--- |
| **Mục Đích Sử Dụng** | Học hiểu bản chất kỹ thuật, tương tác trực quan qua UI | Tự động hóa, tiêu chuẩn hóa đồ án, CI/CD pipeline |
| **Thời Gian Triển Khai** | 45 – 90 phút (thao tác thủ công từng màn hình) | 2 – 3 phút (`terraform apply`) |
| **Xác Suất Sai Sót** | Trung bình - Cao (dễ quên tích checkbox, gõ nhầm port) | 0% (mọi quy tắc được validate trước qua HCL code) |
| **Quản Lý Phiên Bản (GitOps)** | Không thể lưu cấu hình click chuột vào Git | Lưu toàn bộ hạ tầng trong repository dưới dạng code |
| **Rủi Ro Bị Trừ Tiền Sau Khi Dùng** | Dễ quên xóa một dịch vụ ngầm (EBS volume, Elastic IP) | **Bằng 0:** Lệnh `terraform destroy` xóa sạch 100% |
| **Khuyến Nghị Áp Dụng** | **BẮT BUỘC thực hiện lần đầu tiên** để hiểu sâu AWS | **Áp dụng từ lần thứ 2 trở đi** để demo & nộp sản phẩm |

---

## 🎓 7. Tổng Kết Kiến Thức Kỹ Thuật Đạt Được

| Khái Niệm / Dịch Vụ | Vai Trò Trong Dự Án | Giá Trị Học Tập Chuyên Sâu |
| :--- | :--- | :--- |
| **AWS IAM Instance Profile** | Xác thực quyền truy cập S3 cho Spring Boot | Hiểu nguyên lý IAM Role & IMDSv2, loại bỏ hoàn toàn việc lưu Access Key tĩnh. |
| **AWS Security Group** | Tường lửa bảo vệ máy chủ EC2 | Nắm vững cơ chế Stateful Firewall, quy tắc Inbound/Outbound. |
| **AWS S3 Lifecycle Rules** | Quản lý vòng đời lưu trữ file tài liệu | Tự động hóa dọn dẹp version cũ, hiểu bài toán tối ưu chi phí Object Storage. |
| **Linux Swap Tuning** | Cứu nguy RAM cho EC2 t2/t3.micro | Kỹ năng Sysadmin thực chiến: `fallocate`, `mkswap`, `swappiness` khi tài nguyên eo hẹp. |
| **Docker Compose Orchestration** | Chạy toàn bộ 5 dịch vụ trên 1 node | Làm chủ kiến trúc containerized đa dịch vụ (Frontend, Backend, AI, Database). |
| **Cloudflare Edge Proxy** | DNS Anycast, Universal SSL, Anti-DDoS | Hiểu kiến trúc Edge Computing, DNS proxying và bảo vệ IP gốc. |
| **AWS CloudWatch Alarms** | Giám sát tài nguyên CPU & Cảnh báo | Nắm vững kỹ năng Site Reliability Engineering (SRE) và Observability. |
| **Terraform IaC & GitOps** | Mã hóa hạ tầng đám mây dạng khai báo | Thành thạo HCL, quản lý State, tự động hóa provisioning và zero-cost tear-down. |

---

## 8. Khung Ra Quyết Định Kiến Trúc: Khi Nào Chuyển Lên Enterprise Production? (Migration Triggers)

Sau khi hoàn thành giai đoạn học tập, bảo vệ đồ án hoặc kiểm chứng thị trường (MVP) với mức chi phí $0/tháng, khi nào một kiến trúc sư cần đề xuất doanh nghiệp đầu tư ngân sách để nâng cấp lên kiến trúc Production chuẩn?

Bảng sau đây định nghĩa các **ngưỡng kích hoạt (Trigger Thresholds)** khách quan:

| Khía Cạnh Đánh Giá | Trạng Thái Lab MVP ($0/tháng) | Ngưỡng Kích Hoạt Chuyển Lên Production | Hành Động Nâng Cấp Kiến Trúc Tương Ứng |
| :--- | :--- | :--- | :--- |
| **Tải Người Dùng (Concurrency)** | < 10 người dùng thử nghiệm | **$\ge 50$ người dùng đồng thời (CCU)** hoặc **$\ge 20$ requests/giây** liên tục. | Nâng cấp EC2 lên `t4g.medium` (4GB RAM) hoặc `m6i.large` (8GB RAM), bỏ Swap memory để tránh Swap Thrashing. Bật ALB + Auto Scaling Group. |
| **Độ Quan Trọng Dữ Liệu (RPO / RTO)** | Chấp nhận mất dữ liệu thử nghiệm nếu server hỏng. | **Doanh nghiệp yêu cầu:**<br>• RPO < 5 phút (mất mát tối đa 5 phút dữ liệu).<br>• RTO < 5 phút (phục hồi dịch vụ trong 5 phút). | Chuyển toàn bộ database sang **Amazon RDS PostgreSQL Multi-AZ Deployment** có bật Continuous WAL Archiving và PITR. |
| **Tính Sẵn Sàng (Availability / SLA)** | Chấp nhận downtime khi bảo trì, nâng cấp code. | **Hợp đồng khách hàng yêu cầu SLA $\ge 99.9\%$** (tối đa 43 phút gián đoạn/tháng). | Triển khai ALB phân tán trên 2 Availability Zones, chạy tối thiểu 2 instances ứng dụng song song để triển khai Rolling Update không gián đoạn. |
| **Tuân Thủ Bảo Mật (Compliance)** | Kiểm thử nội bộ, IP bảo vệ bằng SG + Cloudflare. | **Cần chứng chỉ bảo mật:**<br>• SOC 2 Type II<br>• ISO 27001<br>• PCI-DSS (thanh toán). | Tách mạng thành Custom 3-Tier VPC, đưa toàn bộ EC2 và RDS vào **Private Subnets**, kích hoạt **AWS NAT Gateway** Multi-AZ, bật AWS WAF. |
| **Quy Mô Vector Search (AI Scale)** | < 5,000 chunks tài liệu | **$\ge 100,000$ chunks vector** hoặc tần suất hỏi đáp RAG dày đặc. | Tách `pgvector` ra khỏi DB giao dịch, chuyển sang **Amazon OpenSearch Service** hoặc **Dedicated Vector DB (Pinecone/Qdrant)** để cách ly tải tính toán. |
| **Chi Phí Cơ Hội Kỹ Sư (TCO Trade-off)** | Kỹ sư sẵn sàng bỏ thời gian tự fix lỗi, tự backup. | **Thời gian kỹ sư sửa lỗi hạ tầng tốn hơn $150 – $300/tháng** (tính theo giờ công lương). | Chuyển dịch toàn diện sang **AWS Managed Services (RDS, ALB, S3 Lifecycle)** để giải phóng năng lực kỹ sư tập trung vào phát triển sản phẩm cốt lõi. |

---

> [!TIP]
> ### 🧭 TỔNG KẾT TƯ DUY KIẾN TRÚC SƯ ĐÁM MÂY (CLOUD ARCHITECT MINDSET)
> Một kỹ sư hoặc kiến trúc sư đám mây xuất sắc **không phải là người tìm mọi cách để ép chi phí về $0 bằng bất cứ giá nào**, mà là người **hiểu sâu sắc từng đồng ngân sách chi ra để mua lại giá trị kỹ thuật gì**:
> 
> - **Ngân sách $0.00 / tháng trong bài lab:** Dạy cho bạn **sự trân trọng tài nguyên**, hiểu cặn kẽ từng bit dữ liệu, từng cổng mạng, từng trang nhớ Linux Swap, từng dòng log, và nắm vững bản chất cốt lõi của các dịch vụ đám mây từ tầng thấp nhất.
> - **Ngân sách $200 – $350 / tháng trong Production:** Là quyết định kinh doanh sáng suốt để mua lại **sự an tâm tuyệt đối, giấc ngủ trọn vẹn của đội ngũ kỹ sư, cam kết bảo toàn dữ liệu khách hàng, và khả năng tự phục hồi bền bỉ của doanh nghiệp trước mọi thảm họa**.


