#import "@preview/merman:0.1.0": show-mermaid-blocks

#set page(
  paper: "a4",
  margin: (x: 1.8cm, top: 2.2cm, bottom: 2.2cm),
  header: align(right, text(fill: rgb("#64748b"), size: 8.5pt)[*AI Engineering / ML / DL / LLM / Agent Keyword Learner*]),
  footer: context [
    #line(length: 100%, stroke: 0.4pt + rgb("#cbd5e1"))
    #grid(
      columns: (1fr, 1fr),
      text(fill: rgb("#64748b"), size: 8.5pt)[Backend Note],
      align(right, text(fill: rgb("#64748b"), size: 8.5pt)[Trang #counter(page).display()]),
    )
  ]
)

#set text(
  font: ("Segoe UI", "Arial"),
  size: 9.5pt,
  lang: "vi",
)

#set par(justify: true, leading: 0.6em)
#set heading(numbering: none)

#show heading.where(level: 1): it => block(
  above: 1.6em, below: 0.9em,
  text(fill: rgb("#0f172a"), weight: "bold", size: 15pt)[
    #it.body
    #v(3pt)
    #line(length: 100%, stroke: 1.5pt + rgb("#2563eb"))
  ]
)

#show heading.where(level: 2): it => block(
  above: 1.3em, below: 0.7em,
  text(fill: rgb("#1e3a8a"), weight: "bold", size: 12pt)[#it.body]
)

#show heading.where(level: 3): it => block(
  above: 1.1em, below: 0.5em,
  text(fill: rgb("#1e40af"), weight: "bold", size: 10.5pt)[#it.body]
)

#show raw.where(lang: "mermaid"): it => align(center, show-mermaid-blocks(width: 85%, error-mode: "placeholder")(it))

// Custom styling functions
#let p0 = box(fill: rgb("#fee2e2"), inset: (x: 3.5pt, y: 1.5pt), radius: 2.5pt, baseline: 0%, text(fill: rgb("#991b1b"), weight: "bold", size: 7.5pt)[P0])
#let p1 = box(fill: rgb("#fef3c7"), inset: (x: 3.5pt, y: 1.5pt), radius: 2.5pt, baseline: 0%, text(fill: rgb("#92400e"), weight: "bold", size: 7.5pt)[P1])
#let p2 = box(fill: rgb("#dbeafe"), inset: (x: 3.5pt, y: 1.5pt), radius: 2.5pt, baseline: 0%, text(fill: rgb("#1e40af"), weight: "bold", size: 7.5pt)[P2])
#let tag(t) = box(fill: rgb("#f1f5f9"), stroke: 0.3pt + rgb("#cbd5e1"), inset: (x: 3.5pt, y: 1.5pt), radius: 2.5pt, baseline: 0%, text(fill: rgb("#334155"), font: ("Consolas"), size: 7.5pt, t))
#let callout(body) = block(
  fill: rgb("#f8fafc"),
  stroke: (left: 3pt + rgb("#2563eb"), rest: 0.4pt + rgb("#e2e8f0")),
  inset: 9pt,
  radius: (right: 4pt),
  width: 100%,
  body
)
#let chk = box(stroke: 0.7pt + rgb("#64748b"), width: 8pt, height: 8pt, radius: 1.5pt, baseline: 10%)

= AI Engineering / ML / DL / LLM / Agent Keyword Learner


#callout[Bản đồ từ khóa học *Machine Learning, Deep Learning, Computer Vision, NLP, LLM, AI Application Engineering và AI Agent Engineering* theo *project-based learning*. Mỗi mục được chia nhỏ thành concept, API, technique, best practice, failure mode, trade-off, lỗi thường gặp hoặc bài lab có thể kiểm chứng.]


*Phạm vi:* tài liệu có hai nhánh bổ trợ nhau. Nhánh *AI Application/Agent Engineering*
đi theo LLM → RAG → tool calling → agent. Nhánh *AI-assisted Model Engineering* bao
gồm Machine Learning, Deep Learning, Computer Vision và NLP ở độ sâu đủ để chọn kỹ
thuật, giao việc cho AI, đọc code, kiểm tra tensor/data flow, đánh giá model và đưa
artifact vào ứng dụng. Nhánh model không phải prerequisite của LLM nhưng vẫn là năng
lực cần học theo project.

*Định hướng implementation:* *Python-first* cho toàn bộ AI Engineering:
LLM application, RAG, evaluation, AI agent, context/harness/loop/graph engineering,
model serving và fine-tuning. Java/Spring Boot chỉ là *integration track thứ cấp*
khi AI service cần kết nối với backend enterprise, business transaction, security
hoặc public API hiện có. Không học hai implementation cho cùng một vertical slice.

*Giới hạn:* mục tiêu không phải tự cài mọi thuật toán, architecture hoặc training loop
từ số 0. AI có thể sinh boilerplate và implementation; người học vẫn phải sở hữu
problem/data contract, split và leakage boundary, input/output shape, metric, threshold,
error analysis, reproducibility và inference contract. Pre-training lớn, GPU kernel,
distributed training và MLOps platform chuyên sâu chỉ học khi project thật sự cần.

*Rà soát gần nhất:* 2026-08-18.

*Baseline tham chiếu:* Python 3.12+ hoặc phiên bản được AI dependencies hỗ trợ;
quản lý project bằng `pyproject.toml` và `uv`; dùng type hints, Pydantic, pytest,
asyncio/httpx và official provider SDK trước khi thêm framework. API, model snapshot,
SDK và orchestration framework thay đổi nhanh: pin dependency, đọc tài liệu đúng
version và chạy eval trước khi thay model/SDK.

#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


== 0. Cách sử dụng


=== Ba mức kiến thức


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Mức*], [*Mục tiêu*], [*Kết quả cần đạt*],
  [*AI/ML Minimum Foundation — 2–3 giờ*], [Chỉ học mental model tối thiểu rồi bắt đầu AI application ngay], [Phân biệt training/inference, split/leakage, pretrained/fine-tuning và hiểu trực giác loss/gradient],
  [*Nhánh song song — AI-assisted ML/DL/CV/NLP*], [Học keyword, kỹ thuật, best practice và workflow kiểm chứng theo project; để AI hỗ trợ implementation], [Tự review được data → model → loss → metric → artifact và phát hiện lỗi logic phổ biến],
  [*Mức 1 — Cần biết để xây AI application*], [Hiểu LLM request/response, prompt, embeddings, RAG, tool calling và kiểm thử căn bản], [Xây một assistant có grounding, structured output, citations và API backend],
  [*Mức 2 — Basic Agent + Production AI*], [Thêm bounded agent loop, state, stop condition, memory, approval; đồng thời productionize context, eval, security và recovery], [Vận hành được AI feature hoặc basic agent với failure model và hard limits rõ ràng],
  [*Mức 3 — Advanced Agent Infrastructure*], [Durable graph, harness, checkpoint/resume, MCP, multi-agent, long-running execution, model serving và bounded autonomy], [Chỉ thêm infrastructure khi có trigger thật; giới hạn blast radius và chứng minh bằng trace/eval],
)
]


#callout[Chỉ phần *AI/ML Minimum Foundation* là bước chuẩn bị ngắn trước Mức 1. Sau đó có thể đi thẳng vào LLM Application hoặc mở nhánh *AI-assisted ML/DL/CV/NLP* theo project. Hai nhánh chạy song song; không cần hoàn thành toàn bộ ML/DL mới được xây RAG/Agent.]


=== Ký hiệu


- #tag("CONCEPT"): khái niệm nền tảng.
- #tag("API"): class, interface, endpoint, schema hoặc method.
- #tag("LIB"): thư viện, SDK hoặc công cụ.
- #tag("BP"): best practice.
- #tag("DP"): design pattern.
- #tag("UTH"): under the hood.
- #tag("PITFALL"): lỗi hoặc hiểu lầm thường gặp.
- #tag("LAB"): bài thực hành nhỏ.
- #tag("STANDARD"): chuẩn/giao thức.
- #tag("CONFIG"): property hoặc configuration knob.
- #tag("METRIC"): metric/eval cần theo dõi.
- #tag("SECURITY"): boundary, threat hoặc control an toàn.
- #tag("TRADEOFF"): đánh đổi phải hiểu trước khi áp dụng.
- #tag("TECHNIQUE"): kỹ thuật thực hành hoặc cách cải thiện pipeline/model.

=== Nhãn độ ưu tiên


- *P0 (Must-Know):* Nằm trên critical path của đúng mức/vertical slice đang học; không có nghĩa phải học toàn bộ P0 của tài liệu trước khi bắt đầu dự án.
- *P1 (Should-Know):* Kiến thức quan trọng khi hệ thống scale, vận hành production hoặc cần reliability cao.
- *P2 (Nice-to-Have):* Kiến thức nâng cao, chuyên sâu hoặc chủ yếu cần cho edge case và hệ thống đặc thù.

#callout[Nhãn ưu tiên là mức độ quan trọng thực tế, không phải thứ tự đọc tuyệt đối. Học P0 theo vertical slice trước, sau đó bổ sung P1/P2 theo failure model và yêu cầu dự án.]


=== Quy tắc học một keyword


+ Nó giải quyết vấn đề gì?
+ Nó nằm ở đâu trong request/context/tool/agent flow?
+ API, schema, class hoặc thư viện nào đại diện cho nó?
+ Failure mode, security risk, cost và trade-off là gì?
+ Có thể chứng minh bằng test, trace, eval, log hoặc metric nào?

```mermaid
flowchart LR
    K[Keyword] --> P[Problem]
    P --> F[Flow position]
    F --> A[API / Schema / Tool]
    A --> R[Failure / Security / Cost]
    R --> E[Eval / Trace / Experiment]
```


=== Bản đồ khái niệm


```mermaid
flowchart TD
    ML[AI/ML minimum foundation] --> LLM[LLM foundations]
    ML --> MODEL[AI-assisted ML/DL/CV/NLP]
    LLM --> PROMPT[Prompt + structured output]
    PROMPT --> RAG[Embedding + RAG]
    RAG --> TOOL[Tool calling]
    TOOL --> AGENT[Basic bounded agent]
    AGENT --> PROD[Production AI]
    PROD --> ADV[Advanced agent infrastructure]
    EVAL[Eval + security + observability] -. xuyên suốt .-> RAG
    EVAL -. xuyên suốt .-> AGENT
    EVAL -. xuyên suốt .-> PROD
    MODEL -. pretrained model / artifact .-> PROD
```


#callout[*Cách đọc:* LLM/RAG/tool calling tạo AI application; ML/DL/CV/NLP tạo hoặc thích nghi model artifact cho use case riêng. Basic agent chỉ thêm một bounded loop có state và stop condition. Harness/graph/checkpoint/MCP/multi-agent là infrastructure nâng cao, chỉ thêm khi basic flow xuất hiện nhu cầu pause/resume, durability, interoperability hoặc delegation thật sự.]


#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= AI/ML MINIMUM FOUNDATION — 2–3 GIỜ, SAU ĐÓ VÀO MỨC 1


#callout[Đây là *foundation ngắn*, không phải mini-course Machine Learning. Học đủ các mục P0 trong phần 0.1 rồi bắt đầu LLM application ngay. Phần 0.2 và 0.3 chỉ mở khi dự án cần đọc model report, làm việc sâu với team ML, fine-tune hoặc xử lý modality ngoài text.]


== 0.1 Critical path trước LLM Application


```mermaid
flowchart TD
    D[Dataset] --> T[Training updates weights]
    T --> M[Pretrained model]
    M --> I[Inference on new input]
    M --> F[Optional fine-tuning]
    F --> I
```


- #chk *ML vs DL vs Generative AI* #p0 #tag("CONCEPT") — ML là phạm vi model học pattern từ dữ liệu; DL dùng neural network nhiều tầng; Generative AI tạo nội dung mới và thường được xây trên deep learning.
- #chk *Model* #p0 #tag("CONCEPT") — Hàm có parameters nhận input và tạo output; AI application thường gọi model đã được train thay vì tự train từ đầu.
- #chk *Dataset / sample* #p0 #tag("CONCEPT") — Tập các ví dụ có schema, nguồn và mục đích rõ; dữ liệu lớn không tự động đồng nghĩa dữ liệu tốt.
- #chk *Training vs inference* #p0 #tag("CONCEPT") — Training cập nhật weights từ dữ liệu; inference giữ weights cố định để xử lý input mới.
- #chk *Train / validation / test split* #p0 #tag("CONCEPT") — Train để học, validation để chọn cấu hình, test làm holdout cuối; không tối ưu lặp lại trên test set.
- #chk *Overfitting* #p0 #tag("CONCEPT") — Model nhớ pattern của train nhưng generalize kém trên dữ liệu chưa thấy.
- #chk *Data leakage* #p0 #tag("PITFALL") — Thông tin từ test/future hoặc sample liên quan lọt sang train làm metric cao giả tạo.
- #chk *Neural network — intuition only* #p0 #tag("CONCEPT") — Nhiều layer có weights biến đổi input thành representation rồi thành prediction; chưa cần tự cài neuron hay backpropagation.
- #chk *Loss — intuition only* #p0 #tag("CONCEPT") — Tín hiệu cho biết prediction lệch mục tiêu ra sao để training có hướng cải thiện; loss không nhất thiết là metric sản phẩm.
- #chk *Gradient descent — intuition only* #p0 #tag("CONCEPT") — Quy trình điều chỉnh weights từng bước theo hướng làm loss giảm; chưa cần đạo hàm hoặc công thức optimizer.
- #chk *Pretrained model* #p0 #tag("CONCEPT") — Model đã học từ dataset lớn và được tái sử dụng qua API, local inference hoặc làm điểm bắt đầu cho adaptation.
- #chk *Fine-tuning — concept only* #p0 #tag("CONCEPT") — Tiếp tục cập nhật một phần/toàn bộ weights bằng dữ liệu mục tiêu; khác RAG vì nó thay đổi model thay vì cung cấp knowledge runtime.

#callout[*Exit nhanh:* tự giải thích được `data → training → pretrained model → inference`; nêu được leakage/overfitting là gì và khi nào fine-tuning khác với chỉ gọi model hoặc dùng RAG. Đạt mức này là đủ để sang Mức 1.]


== 0.2 ML/DL core literacy — dùng lại ở mọi model project


#callout[Những mục dưới đây không phải prerequisite của LLM/RAG/Agent, nhưng là vocabulary bắt buộc khi mở nhánh AI-assisted Model Engineering. Trong nhánh đó, hãy coi các mục P1 ở đây là P0: AI có thể viết code, nhưng không thể thay bạn quyết định split, metric, threshold và ý nghĩa của lỗi.]


=== Metric và vocabulary cho model team


- #chk *Supervised learning* #p1 #tag("CONCEPT") — Model học ánh xạ input/features → label/target từ ví dụ có đáp án.
- #chk *Classification / regression* #p1 #tag("CONCEPT") — Classification dự đoán lớp rời rạc; regression dự đoán giá trị liên tục.
- #chk *Feature / input* #p1 #tag("CONCEPT") — Thông tin model nhận; có thể là tabular feature, ảnh, audio hoặc token.
- #chk *Label / target / ground truth* #p1 #tag("CONCEPT") — Đáp án để train/evaluate; label quality đặt trần cho model quality.
- #chk *Underfitting* #p1 #tag("CONCEPT") — Model kém cả train và validation vì representation hoặc quá trình học chưa đủ.
- #chk *Confusion matrix* #p1 #tag("METRIC") — Đếm TP, FP, FN, TN và luôn gắn positive class với ý nghĩa nghiệp vụ.
- #chk *Accuracy* #p1 #tag("METRIC") — Tỷ lệ đúng tổng thể; dễ gây hiểu nhầm khi class imbalance lớn.
- #chk *Precision / recall / F1* #p1 #tag("METRIC") — Precision tập trung false positive, recall tập trung false negative, F1 cân bằng hai phía; chọn theo cost nghiệp vụ.
- #chk *Decision threshold* #p1 #tag("CONFIG") — Ngưỡng đổi score thành class; thay threshold làm đổi trade-off precision/recall.
- #chk *Class imbalance* #p1 #tag("PITFALL") — Lớp hiếm có thể bị accuracy tổng thể che khuất; cần metric theo class/slice.
- #chk *Baseline* #p1 #tag("BP") — Rule hoặc model đơn giản làm mốc để chứng minh model phức tạp có giá trị.
- #chk *LAB — đọc confusion matrix* #p1 #tag("LAB") — Tính precision/recall/F1 trên ví dụ nhỏ và chọn metric theo cost của FP/FN.


=== Model lifecycle và training details


```mermaid
flowchart TD
    D[Collect + label] --> S[Split data]
    S --> T[Train + validate]
    T --> H[Test holdout]
    H --> X[Export artifact]
    X --> I[Inference]
    I --> M[Monitor + feedback]
```


- #chk *Model checkpoint* #p1 #tag("CONCEPT") — Snapshot trong quá trình train; không mặc nhiên là deployable artifact.
- #chk *Model artifact contract* #p1 #tag("API") — Model version, input/output schema, preprocessing, label map, threshold và runtime compatibility.
- #chk *Data/label versioning* #p1 #tag("BP") — Dataset manifest và labeling guide phải có version/provenance.
- #chk *Experiment reproducibility* #p1 #tag("BP") — Lưu code, data, config, seed, dependency và checkpoint để tái hiện run.
- #chk *Offline–online consistency* #p1 #tag("PITFALL") — Preprocessing khác giữa train và inference làm quality production giảm.
- #chk *Data/model drift* #p1 #tag("CONCEPT") — Distribution input hoặc quan hệ input–label thay đổi theo thời gian.
- #chk *Forward pass* #p1 #tag("CONCEPT") — Đưa batch qua network để tạo prediction và tính loss.
- #chk *Backpropagation* #p2 #tag("UTH") — Truyền gradient từ loss ngược qua layer; framework thường tự động tính.
- #chk *Epoch / batch size / learning rate* #p2 #tag("CONFIG") — Số lượt qua data, số sample mỗi update và độ lớn bước update; chỉ học sâu khi train/fine-tune.
- #chk *Transfer learning* #p1 #tag("CONCEPT") — Tái sử dụng representation đã học cho bài toán mới; fine-tuning là một cách adaptation cụ thể.
- #chk *LAB — pipeline review note* #p1 #tag("LAB") — Vẽ data → train → artifact → inference → monitoring với input, output, owner, version và failure mode.


== 0.3 Chọn track theo dạng output cần tạo


=== Computer Vision — khi input chính là ảnh/video


- #chk *Image classification* #p1 #tag("CONCEPT") — Gán label cho toàn ảnh.
- #chk *Object detection* #p1 #tag("CONCEPT") — Dự đoán class và bounding box cho từng object.
- #chk *Segmentation* #p1 #tag("CONCEPT") — Dự đoán label theo pixel hoặc instance mask.
- #chk *Video/temporal task* #p2 #tag("CONCEPT") — Output phụ thuộc thứ tự, chuyển động hoặc nhiều frame.


=== NLP — khi input/output chính là ngôn ngữ


- #chk *Text/sequence classification* #p1 #tag("CONCEPT") — Gán intent, sentiment, topic hoặc label cho document/sequence.
- #chk *Token classification / NER* #p1 #tag("CONCEPT") — Gán label theo token/span để trích xuất entity.
- #chk *Sequence-to-sequence* #p1 #tag("CONCEPT") — Ánh xạ chuỗi vào chuỗi như dịch, tóm tắt hoặc normalization.
- #chk *Semantic retrieval* #p1 #tag("CONCEPT") — Biểu diễn text thành embedding và tìm nội dung tương tự.


=== Traditional ML — khi dữ liệu chủ yếu là tabular/time series


- #chk *Feature engineering* #p2 #tag("CONCEPT") — Tạo feature tabular/domain trước khi model học.
- #chk *Linear/logistic model* #p2 #tag("CONCEPT") — Baseline dễ giải thích cho regression/classification.
- #chk *Tree / boosting model* #p2 #tag("CONCEPT") — Phù hợp nhiều bài toán tabular; vẫn cần split, leakage control và metric đúng.
- #chk *Clustering / anomaly detection* #p2 #tag("CONCEPT") — Học cấu trúc hoặc phát hiện điểm bất thường khi label thiếu/không đầy đủ.
- #chk *Time-aware split* #p1 #tag("BP") — Dự báo tương lai phải split theo thời gian; random split có thể nhìn thấy tương lai.


#callout[Chọn track bằng *unit of prediction* và dạng output, không chọn vì architecture đang nổi. Chi tiết workflow, keyword, kỹ thuật và best practice của ML/DL/CV/NLP nằm ở nhánh song song ngay sau foundation. Audio/RL/time-series nâng cao vẫn học on-demand khi project yêu cầu.]


== 0.4 Ranh giới hoàn thành foundation


*Cần trước Mức 1:*

- Phân biệt ML, DL, Generative AI; training và inference.
- Giải thích vai trò của dataset, train/validation/test, overfitting và leakage.
- Hiểu trực giác neural network, loss, gradient descent, pretrained model và fine-tuning.

*Học trong nhánh AI-assisted Model Engineering khi có project:*

- Precision/recall/F1, threshold, class imbalance và model review workflow.
- Backpropagation, epoch/batch/learning rate, transfer learning và model artifact sâu.
- ML truyền thống, PyTorch/DL core, CV hoặc NLP theo đúng dạng bài toán.
- Audio, reinforcement learning, training từ đầu, distributed training hoặc GPU/MLOps internals chỉ khi có trigger cụ thể.

#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= NHÁNH SONG SONG — AI-ASSISTED ML/DL/CV/NLP


#callout[Mục tiêu của nhánh này không phải “AI code hộ nên không cần hiểu”. Mục tiêu là chuyển effort từ nhớ cú pháp và viết boilerplate sang *framing, data, shape, metric, diagnosis và verification*. Bạn cần đọc được vertical slice của model project, giải thích được quyết định chính và yêu cầu AI sửa bằng bằng chứng thay vì thử ngẫu nhiên.]


== A. Hợp đồng làm việc: AI triển khai, engineer sở hữu tính đúng


#align(center)[
#table(
  columns: (0.85fr, 1.15fr, 1.25fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Giai đoạn*], [*AI hỗ trợ tốt*], [*Bạn phải tự sở hữu*],
  [Problem], [Gợi ý formulation, baseline, metric và edge case], [Unit of prediction, target, constraint, cost của FP/FN và success criteria],
  [Data], [Sinh loader, transform, EDA/visualization và validation script], [Nguồn dữ liệu, schema, split group/time, leakage boundary, label semantics và quyền sử dụng],
  [Model], [Scaffold architecture/pretrained model, training loop, config và tests], [Input/output shape, loss–activation pairing, preprocessing, assumptions và trade-off],
  [Evaluation], [Tính metric, plot confusion/error case và tạo report], [Metric chính, threshold, slice, holdout discipline và kết luận model có dùng được hay không],
  [Debug], [Phân tích stack trace, đề xuất experiment và patch code], [Tái hiện lỗi, đổi một biến mỗi experiment và bác bỏ fix không có evidence],
  [Deployment], [Export, API wrapper, benchmark và container boilerplate], [Artifact contract, inference parity, latency/memory budget, monitoring và rollback],
)
]


=== A.1 Ba độ sâu cần đạt


- #chk *Explain* #p0 #tag("CONCEPT") — Giải thích keyword bằng problem → mechanism → trade-off → evidence; không cần thuộc công thức khi nó chưa thay đổi quyết định.
- #chk *Inspect* #p0 #tag("BP") — Theo được data flow và tensor shape từ sample → batch → model → loss → decode → metric.
- #chk *Adapt* #p0 #tag("BP") — Đổi dataset/schema/num classes/pretrained model/metric/threshold mà không phá contract.
- #chk *Implement from scratch* #p2 #tag("TRADEOFF") — Chỉ làm khi mục tiêu học tập hoặc yêu cầu bài tập bắt buộc; production ưu tiên implementation đã được kiểm chứng.


#callout[*Ranh giới hợp lý:* không cần nhớ chính xác mọi tham số API, tự viết autograd, CNN, optimizer hay YOLO từ trí nhớ. Nhưng phải phát hiện được split sai, tensor sai shape/dtype, loss không khớp output, metric sai semantics, preprocessing lệch train–inference và kết luận vượt quá evidence.]


=== A.2 Evidence-first workflow


```mermaid
flowchart TD
    P[Problem + success metric] --> D[Data contract + split]
    D --> B[Simple baseline]
    B --> C[AI-assisted implementation]
    C --> S[Smoke + tiny-set tests]
    S --> E[Holdout evaluation]
    E --> R[Error analysis]
    R --> A[Artifact + inference contract]
```


- #chk *One change per experiment* #p0 #tag("BP") — Nếu đổi model, augmentation, loss và learning rate cùng lúc thì không biết nguyên nhân cải thiện.
- #chk *Baseline before complexity* #p0 #tag("BP") — Rule, majority class, linear model hoặc pretrained head đơn giản phải có trước architecture phức tạp.
- #chk *Evidence bundle* #p0 #tag("BP") — Mỗi run lưu config, data version, code commit, seed, metric theo split/slice, checkpoint và một số prediction mẫu.
- #chk *No silent assumption* #p0 #tag("BP") — Yêu cầu AI liệt kê assumptions, expected shapes, label encoding và library versions trước khi code.
- #chk *No metric-only acceptance* #p0 #tag("PITFALL") — Metric tổng đẹp chưa đủ; phải xem error case, class/slice yếu, calibration và inference parity.


== B. Workflow chung cho mọi model project


=== B.1 Problem formulation và data contract


- #chk *Unit of prediction* #p0 #tag("CONCEPT") — Một row, ảnh, bounding box, pixel, token, span, document hay time window; unit sai kéo theo split và metric sai.
- #chk *Task formulation* #p0 #tag("CONCEPT") — Classification, regression, ranking, detection, segmentation, sequence labeling, generation, clustering hoặc anomaly detection.
- #chk *Target/label taxonomy* #p0 #tag("API") — Định nghĩa class, `id ↔ label`, unknown/other, multi-class vs multi-label và policy cho ambiguous sample.
- #chk *Data schema* #p0 #tag("API") — Field, type, shape, range, nullable, units, coordinate system, encoding và version.
- #chk *Sampling unit và split group* #p0 #tag("BP") — Các sample cùng user, patient, video, document hoặc source phải ở cùng split nếu chúng có thể tiết lộ lẫn nhau.
- #chk *Random / stratified / grouped / time split* #p0 #tag("TECHNIQUE") — Chọn theo cách dữ liệu xuất hiện ngoài đời; không dùng random split theo thói quen.
- #chk *Label leakage / proxy leakage* #p0 #tag("PITFALL") — Feature có trực tiếp hoặc gián tiếp chứa target/future outcome làm model “đoán đúng” nhưng không deploy được.
- #chk *Duplicate / near-duplicate detection* #p0 #tag("BP") — Hash, perceptual hash, text similarity hoặc group ID trước khi split.
- #chk *Annotation guideline* #p0 #tag("BP") — Ví dụ đúng/sai, boundary case, label priority và quy trình adjudication; agreement thấp báo hiệu target chưa ổn định.
- #chk *Data provenance và license* #p0 #tag("SECURITY") — Nguồn, consent, license, PII/sensitive data, retention và quyền dùng để train/deploy.
- #chk *Dataset card* #p1 #tag("BP") — Mô tả nguồn, population, collection, schema, split, limitation và known bias.


=== B.2 Preprocessing và feature pipeline


- #chk *Fit only on train* #p0 #tag("BP") — Imputer, scaler, vocabulary, feature selection và statistics phải `fit` trên train rồi chỉ `transform` validation/test.
- #chk *Deterministic validation/test transform* #p0 #tag("BP") — Random augmentation chỉ dành cho train; eval cần pipeline ổn định.
- #chk *Train–serve parity* #p0 #tag("BP") — Cùng tokenizer, resize, normalization, label map, feature order và postprocessing ở offline/online.
- #chk *Missing/outlier policy* #p0 #tag("CONCEPT") — Xử lý thiếu và outlier theo ý nghĩa domain, không mặc định drop hoặc fill mean.
- #chk *Class imbalance* #p0 #tag("CONCEPT") — Cân nhắc class weights, resampling, focal loss hoặc threshold; luôn báo metric theo class.
- #chk *Data augmentation* #p1 #tag("TECHNIQUE") — Tạo biến thể bảo toàn label; augmentation không hợp domain có thể thêm label noise.
- #chk *Preprocessing pipeline as artifact* #p0 #tag("DP") — Đóng gói preprocessing cùng model thay vì copy logic vào notebook và API riêng.


=== B.3 Training và model selection


- #chk *Simple baseline* #p0 #tag("BP") — Chứng minh dữ liệu/metric/pipeline chạy đúng trước khi tối ưu model.
- #chk *Pretrained model / transfer learning* #p0 #tag("TECHNIQUE") — Bắt đầu từ weights đã học; thay head và fine-tune theo dữ liệu mục tiêu.
- #chk *Freeze / unfreeze* #p1 #tag("TECHNIQUE") — Train head trước, mở dần backbone khi dữ liệu đủ và baseline đã ổn.
- #chk *Loss–task alignment* #p0 #tag("BP") — Loss phải khớp target encoding và output semantics: multi-class, multi-label, regression, boxes, masks hoặc tokens.
- #chk *Optimizer / learning rate* #p0 #tag("CONFIG") — Learning rate thường ảnh hưởng mạnh hơn việc đổi optimizer; log giá trị và schedule thực tế.
- #chk *Early stopping* #p1 #tag("TECHNIQUE") — Dừng theo validation metric với patience; không quan sát test để quyết định dừng.
- #chk *Regularization* #p1 #tag("TECHNIQUE") — Weight decay, dropout, augmentation, label smoothing hoặc model nhỏ hơn để giảm overfitting.
- #chk *Hyperparameter search* #p1 #tag("TECHNIQUE") — Random/Bayesian search trên validation hoặc cross-validation; test vẫn giữ kín.
- #chk *Checkpoint best vs last* #p0 #tag("CONCEPT") — `best` theo metric dùng cho selection; `last` dùng resume; hai artifact có mục đích khác nhau.
- #chk *Reproducibility envelope* #p1 #tag("BP") — Seed giúp so sánh nhưng không bảo đảm tuyệt đối trên mọi device/kernel; lưu environment và chấp nhận tolerance.


=== B.4 Evaluation, threshold và error analysis


- #chk *Offline metric vs business metric* #p0 #tag("TRADEOFF") — F1/mAP/Dice chỉ là proxy; map chúng vào missed case, review load, cost, safety hoặc conversion.
- #chk *Threshold tuning* #p0 #tag("TECHNIQUE") — Chọn trên validation theo cost/constraint; không chọn threshold trên test.
- #chk *Calibration* #p1 #tag("CONCEPT") — Score `0.8` chỉ hữu ích như confidence nếu các case tương tự đúng xấp xỉ 80%; calibration khác ranking quality.
- #chk *Slice evaluation* #p0 #tag("BP") — Báo metric theo class, source, language, device, lighting, size, length hoặc nhóm rủi ro.
- #chk *Error taxonomy* #p0 #tag("TECHNIQUE") — Nhóm lỗi data/label, preprocessing, confusion, coverage, threshold, domain shift và postprocessing trước khi chọn fix.
- #chk *Ablation* #p1 #tag("TECHNIQUE") — Bỏ từng component/feature để kiểm tra nó thật sự tạo giá trị.
- #chk *Confidence interval / repeated run* #p2 #tag("METRIC") — Với dataset nhỏ hoặc variance cao, báo độ bất định thay vì một con số duy nhất.
- #chk *Human review sample* #p0 #tag("BP") — Xem prediction đúng/sai/ngập ngừng và sample model không dự đoán được; metric không thay thế semantic review.


=== B.5 Artifact, inference và monitoring


- #chk *Inference contract* #p0 #tag("API") — Model version, input schema, preprocessing, output schema, label map, threshold, postprocessing và runtime/device.
- #chk *Offline–online parity test* #p0 #tag("LAB") — Cùng một fixture phải cho output tương đương giữa notebook/batch job/API/exported runtime trong tolerance đã định.
- #chk *Batching* #p1 #tag("TECHNIQUE") — Tăng throughput nhưng có thể tăng latency/memory; benchmark theo workload thật.
- #chk *Export format* #p1 #tag("CONCEPT") — Native checkpoint, TorchScript/`torch.export`, ONNX hoặc runtime-specific artifact; chọn theo target, operator support và parity.
- #chk *Model/data version registry* #p1 #tag("BP") — Biết production đang dùng code, data, weights, preprocessing và threshold nào.
- #chk *Shadow / canary* #p1 #tag("DP") — So sánh model mới trên traffic thật trước rollout đầy đủ; có rollback artifact.
- #chk *Input drift / prediction drift / performance drift* #p1 #tag("METRIC") — Distribution thay đổi không đồng nghĩa quality chắc chắn giảm; cần label feedback hoặc proxy đã được kiểm chứng.
- #chk *Abstain / human fallback* #p0 #tag("BP") — Khi confidence/quality/safety không đủ, trả unknown hoặc chuyển review thay vì ép prediction.


== C. Machine Learning truyền thống — keyword và kỹ thuật đủ dùng


=== C.1 Chọn family model


#align(center)[
#table(
  columns: (0.9fr, 1.1fr, 1.2fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Use case*], [*Baseline nên thử*], [*Điểm cần kiểm tra*],
  [Tabular classification], [Logistic regression → tree/gradient boosting], [Class imbalance, calibration, threshold, leakage],
  [Tabular regression], [Mean/median → linear model → boosting], [MAE/RMSE, outlier, residual theo slice],
  [Sparse text features], [TF-IDF + logistic/linear SVM], [Vocabulary leakage, n-gram, macro-F1],
  [Clustering], [K-means hoặc density-based method], [Scaling, distance semantics, stability, business meaning],
  [Anomaly detection], [Rule/statistical baseline → isolation-based method], [Rare labels, alert rate, false-positive workload],
  [Time series], [Naive/seasonal baseline → feature/model phù hợp], [Walk-forward split, horizon, future leakage],
)
]


- #chk *Linear/logistic model* #p0 #tag("CONCEPT") — Baseline nhanh, dễ diễn giải; cần scaling cho nhiều thuật toán tuyến tính/distance-based.
- #chk *Decision tree / random forest* #p1 #tag("CONCEPT") — Học rule phi tuyến; tree đơn dễ overfit, ensemble ổn định hơn nhưng nặng hơn.
- #chk *Gradient boosting* #p1 #tag("CONCEPT") — Baseline mạnh cho tabular; tune depth/learning rate/estimators và kiểm soát leakage trước.
- #chk *K-nearest neighbors / distance model* #p2 #tag("CONCEPT") — Nhạy scaling, dimension và metric khoảng cách.
- #chk *Clustering* #p1 #tag("CONCEPT") — Không có ground-truth mặc định; cluster phải được kiểm tra stability và ý nghĩa domain.
- #chk *Dimensionality reduction* #p2 #tag("TECHNIQUE") — PCA/UMAP-style projection hỗ trợ compression/visualization; hình đẹp không tự chứng minh cluster thật.


=== C.2 scikit-learn patterns cần nhận ra


- #chk *Estimator API* #p0 #tag("API") — `fit`, `predict`, `predict_proba`, `transform`; hiểu object nào học statistics và object nào chỉ biến đổi.
- #chk *Pipeline* #p0 #tag("DP") — Ghép preprocessing + model để cross-validation và inference không quên bước hoặc leak statistics.
- #chk *ColumnTransformer* #p0 #tag("API") — Pipeline riêng cho numeric/categorical/text columns trong cùng schema.
- #chk *Imputation / scaling / encoding* #p0 #tag("TECHNIQUE") — Xử lý missing, scale numeric và encode categorical; fit trên train trong pipeline.
- #chk *Cross-validation* #p0 #tag("TECHNIQUE") — Ước lượng ổn định hơn trên data nhỏ; dùng stratified/group/time-aware splitter đúng semantics.
- #chk *Grid/random search* #p1 #tag("TECHNIQUE") — Search cả pipeline trong CV; không tune trực tiếp trên holdout test.
- #chk *Class weights / sample weights* #p1 #tag("TECHNIQUE") — Thay cost contribution của sample/class; không bảo đảm probability đã calibrated.
- #chk *Feature importance / permutation importance / SHAP-style explanation* #p1 #tag("CONCEPT") — Dùng để điều tra, không mặc định là causal explanation.
- #chk *Model persistence* #p1 #tag("BP") — Lưu pipeline, version thư viện và schema; serialized artifact không phải dữ liệu an toàn để load từ nguồn không tin cậy.


== D. Deep Learning và PyTorch literacy để review code AI


=== D.1 Tensor và model contract


- #chk *Tensor shape* #p0 #tag("CONCEPT") — Luôn ghi tên axis, ví dụ image batch `B × C × H × W`, token batch `B × L`; shape đúng quan trọng hơn thuộc class API.
- #chk *dtype* #p0 #tag("CONCEPT") — Feature thường float, class index thường integer, mask/boolean tùy loss; sai dtype có thể crash hoặc silently change semantics.
- #chk *device* #p0 #tag("CONCEPT") — Model, input và target phải ở device tương thích; data loading vẫn thường bắt đầu trên CPU.
- #chk *`nn.Module` / `forward`* #p0 #tag("API") — Module đăng ký parameters/submodules; `forward` mô tả data flow, không chứa metric/reporting side effect.
- #chk *Parameters / buffers / `state_dict`* #p0 #tag("CONCEPT") — Parameters được optimize; buffers như running statistics vẫn thuộc model state; `state_dict` là contract checkpoint phổ biến.
- #chk *Logits* #p0 #tag("CONCEPT") — Raw scores trước activation; nhiều loss nhận logits trực tiếp để ổn định số học.
- #chk *Dataset / DataLoader / collate* #p0 #tag("API") — Dataset tạo sample, collate ghép batch, DataLoader quản lý batching/shuffle/workers; variable-length data cần collate/padding riêng.


=== D.2 Training loop phải đọc được


```mermaid
flowchart TD
    B[Load batch] --> F[Forward → logits]
    F --> L[Compute loss]
    L --> G[zero_grad → backward]
    G --> U[optimizer step]
    U --> V[Validation in eval mode]
```


- #chk *`model.train()` vs `model.eval()`* #p0 #tag("API") — Đổi behavior của dropout/batch normalization; không đồng nghĩa bật/tắt gradient.
- #chk *`inference_mode` / `no_grad`* #p0 #tag("API") — Tắt gradient tracking khi validation/inference để giảm memory; vẫn cần `model.eval()`.
- #chk *`zero_grad → backward → step`* #p0 #tag("UTH") — Gradient mặc định tích lũy; thứ tự sai làm update sai hoặc giữ gradient ngoài ý muốn.
- #chk *Batch / epoch / iteration* #p0 #tag("CONCEPT") — Iteration xử lý một batch; epoch đi qua dataset; log phải phân biệt rõ.
- #chk *Gradient accumulation* #p1 #tag("TECHNIQUE") — Mô phỏng effective batch lớn hơn khi thiếu memory; phải scale loss/step/scheduler đúng.
- #chk *Mixed precision* #p1 #tag("TECHNIQUE") — Giảm memory/tăng tốc trên hardware phù hợp; theo dõi overflow/NaN và parity.
- #chk *Gradient clipping* #p1 #tag("TECHNIQUE") — Giới hạn gradient norm/value để giảm exploding gradients; không chữa learning rate hoặc data lỗi.
- #chk *Learning-rate scheduler* #p1 #tag("TECHNIQUE") — Thay learning rate theo step/epoch/metric; log thời điểm `scheduler.step()`.
- #chk *Checkpoint resume* #p0 #tag("BP") — Muốn resume đúng phải lưu model, optimizer, scheduler/scaler, epoch/step, RNG/config; chỉ weights là chưa đủ.


=== D.3 Loss, activation và target pairing


- #chk *Multi-class* #p0 #tag("BP") — Một class/sample: output logits `B × C`, target class index `B`; softmax thường chỉ dùng để diễn giải/inference.
- #chk *Multi-label* #p0 #tag("BP") — Nhiều class/sample: output logits `B × C`, target multi-hot; sigmoid theo class và threshold riêng nếu cần.
- #chk *Binary classification* #p0 #tag("BP") — Có thể dùng một logit + binary loss hoặc hai logits + multi-class loss; target/shape phải nhất quán.
- #chk *Regression* #p0 #tag("BP") — Output continuous; MAE/MSE/Huber phản ánh sensitivity với outlier khác nhau.
- #chk *Ignore index / padding mask* #p1 #tag("CONFIG") — Không tính loss trên padded/void label; phải đồng bộ với metric.
- #chk *Numerical stability* #p1 #tag("PITFALL") — Ưu tiên loss nhận logits; tránh tự softmax/sigmoid rồi log nếu API đã gộp operation ổn định.


=== D.4 Debugging ladder — yêu cầu AI chứng minh từng bậc


+ #chk In 3–5 sample sau preprocessing cùng label/target; kiểm tra range, shape, dtype và semantics.
+ #chk Chạy một batch qua model; assert output/target shape và loss hữu hạn.
+ #chk Overfit một batch hoặc tập rất nhỏ; nếu không làm được, pipeline/model/loss/optimizer có lỗi trước khi nói về generalization.
+ #chk So sánh với baseline đơn giản và random/majority behavior.
+ #chk Theo dõi train/validation loss + metric; phân biệt underfit, overfit và optimization instability.
+ #chk Kiểm tra prediction distribution, per-class count, confusion/error examples và NaN/Inf.
+ #chk Chạy inference từ checkpoint mới load trong process sạch; so với output trước khi save.


- #chk *Shape assertion* #p0 #tag("BP") — Assert sớm ở dataset/collate/model/loss boundary thay vì để lỗi lan tới metric.
- #chk *Tiny-set overfit test* #p0 #tag("LAB") — Test integration mạnh cho DL pipeline; nó không chứng minh model generalize.
- #chk *Train–eval mode bug* #p0 #tag("PITFALL") — Quên `eval()` hoặc dùng augmentation ngẫu nhiên trong validation làm metric không ổn định.
- #chk *Detached graph / accidental no-grad* #p1 #tag("PITFALL") — Convert sang NumPy, `.item()` hoặc detach sai chỗ có thể cắt gradient.
- #chk *OOM is a system constraint* #p1 #tag("PITFALL") — Giảm batch/resolution/sequence length, dùng accumulation/AMP hoặc model nhỏ hơn; không retry mù.


== E. Computer Vision — keyword, kỹ thuật và best practice


=== E.1 Chọn task và output contract


#align(center)[
#table(
  columns: (0.8fr, 1fr, 1.15fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Task*], [*Target*], [*Output điển hình*], [*Metric chính*],
  [Classification], [Một/multiple label cho ảnh], [Class logits/probabilities], [Accuracy, macro-F1, AUROC theo bài toán],
  [Object detection], [Class + bounding box/object], [Boxes, scores, labels], [mAP theo IoU, precision/recall],
  [Semantic segmentation], [Class/pixel], [Mask logits `C × H × W`], [mIoU, Dice, per-class IoU],
  [Instance segmentation], [Box + mask/object], [Boxes, scores, labels, masks], [Box mAP + mask mAP],
  [OCR], [Text region + sequence], [Boxes/polygons + text], [Detection metric + CER/WER],
)
]


- #chk *Image tensor layout* #p0 #tag("CONCEPT") — Phân biệt `HWC`, `CHW`, batched `BCHW`, color channel và numeric range.
- #chk *Resolution / aspect ratio* #p0 #tag("TRADEOFF") — Resolution cao giữ small object/detail nhưng tăng memory, latency và compute.
- #chk *Resize / crop / pad / letterbox* #p0 #tag("TECHNIQUE") — Thay đổi geometry khác nhau; box/mask/keypoint phải biến đổi đồng bộ và có thể map về ảnh gốc.
- #chk *Normalization* #p0 #tag("BP") — Dùng đúng range/mean/std mà pretrained weights yêu cầu; mismatch làm representation giảm chất lượng.
- #chk *Interpolation* #p0 #tag("BP") — Image có thể bilinear/bicubic; categorical mask thường dùng nearest-neighbor để không sinh class ID giả.
- #chk *Augmentation policy* #p1 #tag("TECHNIQUE") — Flip/crop/color/blur/noise/MixUp/CutMix chỉ dùng khi label invariance hợp domain.
- #chk *Synchronized transform* #p0 #tag("BP") — Với detection/segmentation, cùng random transform phải áp dụng lên image và target geometry.


=== E.2 Architecture vocabulary — hiểu vai trò, không cần tự cài


- #chk *CNN / convolution* #p1 #tag("CONCEPT") — Học local spatial pattern với weight sharing; vẫn là backbone phổ biến và hiệu quả.
- #chk *Vision Transformer / patch* #p1 #tag("CONCEPT") — Biến ảnh thành patch tokens và dùng attention; preprocessing/resolution/weights contract vẫn quyết định kết quả.
- #chk *Backbone* #p0 #tag("CONCEPT") — Trích xuất feature maps từ ảnh.
- #chk *Neck / feature pyramid* #p1 #tag("CONCEPT") — Kết hợp feature nhiều scale để xử lý object/kết cấu kích thước khác nhau.
- #chk *Head* #p0 #tag("CONCEPT") — Chuyển feature thành class, box, mask hoặc keypoint prediction.
- #chk *Stride / downsampling* #p1 #tag("CONCEPT") — Feature map nhỏ hơn input; stride lớn tăng receptive field/efficiency nhưng mất detail.
- #chk *Receptive field* #p1 #tag("CONCEPT") — Vùng input có thể ảnh hưởng một feature; local detail và global context cần cân bằng.
- #chk *Pretrained weights metadata* #p0 #tag("API") — Categories, preprocessing transform, expected input, recipe và license phải đi cùng weights.


=== E.3 Object detection


- #chk *Bounding-box format* #p0 #tag("API") — `xyxy`, `xywh`, absolute hay normalized; ghi rõ coordinate origin và image size.
- #chk *IoU* #p0 #tag("METRIC") — Mức overlap giữa predicted/ground-truth box; dùng cho matching, NMS và AP threshold.
- #chk *Objectness / class score* #p0 #tag("CONCEPT") — Khả năng có object và class confidence có semantics khác nhau tùy architecture.
- #chk *Anchor-based / anchor-free* #p1 #tag("CONCEPT") — Hai family dự đoán box; anchors thêm size/aspect prior và assignment logic.
- #chk *Positive/negative assignment* #p1 #tag("UTH") — Quy tắc gán target cho prediction location/anchor ảnh hưởng mạnh tới loss và imbalance.
- #chk *Box regression loss* #p1 #tag("CONCEPT") — L1/Smooth-L1/IoU-family loss tối ưu geometry theo cách khác nhau.
- #chk *Non-Maximum Suppression (NMS)* #p0 #tag("TECHNIQUE") — Loại prediction trùng theo score + IoU; threshold quá thấp có thể xóa object gần nhau.
- #chk *mAP* #p0 #tag("METRIC") — Trung bình AP theo class và có thể theo nhiều IoU thresholds; luôn ghi rõ protocol/version.
- #chk *Small/medium/large-object slices* #p1 #tag("METRIC") — Metric tổng có thể che việc model gần như không thấy small object.
- #chk *Empty image / empty target* #p0 #tag("PITFALL") — Loader, loss, metric và batch phải hỗ trợ ảnh không có object.


=== E.4 Segmentation


- #chk *Semantic / instance / panoptic segmentation* #p0 #tag("CONCEPT") — Semantic gán class/pixel, instance tách từng object, panoptic kết hợp stuff + things.
- #chk *Mask encoding* #p0 #tag("API") — Class-index mask, one-hot, binary-per-class hoặc polygons/RLE; loss và metric phải dùng đúng representation.
- #chk *Pixel logits và argmax/threshold* #p0 #tag("CONCEPT") — Multi-class thường argmax theo channel; binary/multi-label thường sigmoid + threshold.
- #chk *Ignore/void label* #p0 #tag("CONFIG") — Pixel không đánh giá phải bị bỏ cả trong loss và metric.
- #chk *IoU / Dice* #p0 #tag("METRIC") — Đo overlap; cần quy ước rõ cho class vắng mặt/background.
- #chk *Class imbalance theo pixel* #p1 #tag("PITFALL") — Background lớn làm accuracy pixel vô nghĩa; dùng class-aware metric/loss.
- #chk *Boundary error* #p1 #tag("CONCEPT") — IoU tương tự có thể che chất lượng boundary khác nhau; visualize overlay và dùng boundary metric nếu domain cần.


=== E.5 CV verification checklist


+ #chk Visualize raw image + target trước transform và sau transform.
+ #chk Kiểm tra label map, box validity, mask unique values và sample không có target.
+ #chk Split theo scene/source/video/entity trước khi tách frame hoặc crop để tránh near-duplicate leakage.
+ #chk Dùng pretrained preprocessing chính xác; augmentation chỉ ở train.
+ #chk Overfit tiny set rồi mới train toàn bộ; xem prediction overlay mỗi epoch/checkpoint quan trọng.
+ #chk Báo metric per class, size, source/lighting/device và xem false positive/false negative.
+ #chk Benchmark cả preprocessing + model + NMS/postprocessing trên target device.
+ #chk Test map box/mask từ resized space về original coordinates.


#callout[*Khi yêu cầu AI code CV:* bắt AI xuất “shape + coordinate table” cho từng boundary: raw annotation → dataset target → augmented target → model input/output → decoded prediction → metric. Phần lớn bug CV khó chịu nằm ở geometry/format, không nằm trong backbone.]


== F. NLP — từ classical pipeline tới pretrained Transformer


=== F.1 Chọn task theo output


#align(center)[
#table(
  columns: (0.9fr, 1.1fr, 1.15fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Task*], [*Output*], [*Metric/điểm cần kiểm tra*],
  [Sequence classification], [Một hoặc nhiều label/document], [Macro/micro-F1, threshold, length/language slices],
  [Token classification / NER], [Label/token hoặc entity span], [Entity-level precision/recall/F1, label alignment],
  [Sequence-to-sequence], [Output text sequence], [Task metric + human/semantic review],
  [Semantic similarity/retrieval], [Embedding hoặc ranked candidates], [Recall\@k, MRR/nDCG, hard negatives],
  [Language modeling/generation], [Token sequence], [Perplexity chỉ là proxy; factuality/safety/task eval riêng],
)
]


- #chk *Corpus / document / sentence / span / token* #p0 #tag("CONCEPT") — Các unit khác nhau kéo theo annotation, split và evaluation khác nhau.
- #chk *Text normalization* #p0 #tag("TRADEOFF") — Unicode, casing, whitespace, accent, emoji và punctuation; normalize quá mạnh có thể xóa tín hiệu.
- #chk *Tokenization / subword* #p0 #tag("CONCEPT") — Text → tokens → IDs; một từ có thể thành nhiều subword.
- #chk *Vocabulary / unknown token* #p1 #tag("CONCEPT") — Classical word vocab có OOV; subword giảm OOV nhưng tăng sequence length.
- #chk *Padding / truncation / attention mask* #p0 #tag("API") — Padding ghép batch, truncation giới hạn length, mask cho model biết vị trí hợp lệ.
- #chk *Special tokens* #p1 #tag("CONCEPT") — BOS/EOS/CLS/SEP/PAD và format phụ thuộc tokenizer/model.
- #chk *Static vs contextual embedding* #p1 #tag("CONCEPT") — Static vector cố định theo token; contextual representation thay đổi theo context.
- #chk *Encoder / decoder / encoder–decoder* #p1 #tag("CONCEPT") — Encoder phù hợp understanding, decoder cho generation, encoder–decoder cho input→output sequence; không phải luật tuyệt đối.
- #chk *Pooling* #p1 #tag("TECHNIQUE") — CLS/mean/max/learned pooling đổi token representations thành sequence vector.


=== F.2 Classical NLP, pretrained model hay LLM?


- #chk *Rule/regex/dictionary* #p0 #tag("TRADEOFF") — Dùng khi pattern rõ, yêu cầu giải thích cao và coverage kiểm soát được.
- #chk *TF-IDF + linear model* #p0 #tag("TECHNIQUE") — Baseline mạnh, rẻ và dễ debug cho classification trên dataset vừa/nhỏ.
- #chk *Pretrained encoder fine-tuning* #p0 #tag("TECHNIQUE") — Phù hợp classification/NER/embedding chuyên biệt khi có labeled data và cần latency/cost ổn định.
- #chk *Embedding + retrieval* #p0 #tag("TECHNIQUE") — Phù hợp semantic search/matching; đánh giá retrieval thay vì chỉ cosine mẫu.
- #chk *Prompted LLM* #p0 #tag("TRADEOFF") — Phù hợp open-ended generation/extraction ít data; cần schema, grounding, eval, latency/cost/safety controls.
- #chk *Hybrid pipeline* #p1 #tag("DP") — Rule/small model cho routing/filter, retrieval cung cấp evidence, LLM xử lý case mơ hồ hoặc synthesis.


=== F.3 Fine-tuning và data best practices


- #chk *Tokenizer–model pairing* #p0 #tag("BP") — Không tự thay tokenizer/vocab của checkpoint nếu chưa hiểu embedding/config contract.
- #chk *Label alignment cho NER* #p0 #tag("BP") — Word label phải map sang subword; quy ước first-subword/all-subwords/ignore phải đồng bộ với metric.
- #chk *Sequence length distribution* #p0 #tag("METRIC") — Đo percentiles và truncation rate trước khi chọn `max_length`; long document cần chunk/hierarchy/retrieval.
- #chk *Dynamic padding / data collator* #p1 #tag("TECHNIQUE") — Pad theo batch để tiết kiệm compute; theo dõi shape variability.
- #chk *Full fine-tuning vs PEFT/LoRA* #p1 #tag("TRADEOFF") — PEFT giảm trainable parameters và artifact size; không miễn eval hoặc license/deployment review.
- #chk *Class imbalance / hard negatives* #p1 #tag("TECHNIQUE") — Sampling, loss weights và hard-negative mining có thể cải thiện boundary; tránh đưa false negatives vào data.
- #chk *Split by author/conversation/source/time* #p0 #tag("BP") — Random sentence split dễ leak style, template hoặc cùng conversation sang test.
- #chk *Exact/near-duplicate text* #p0 #tag("PITFALL") — Template và paraphrase lặp làm metric cao giả; deduplicate trước split khi phù hợp.
- #chk *Multilingual slice* #p0 #tag("BP") — Đánh giá theo language/script/code-switching; average metric che ngôn ngữ yếu.
- #chk *PII/toxic/licensed content* #p0 #tag("SECURITY") — Redact/control access, ghi provenance và kiểm tra data/model license trước fine-tuning.


=== F.4 NLP evaluation pitfalls


- #chk *Macro vs micro F1* #p0 #tag("METRIC") — Macro cho mỗi class trọng lượng bằng nhau; micro ưu tiên class/sample phổ biến.
- #chk *Entity-level vs token-level F1* #p0 #tag("METRIC") — NER cần đúng span/type ở entity level; token accuracy có thể quá lạc quan.
- #chk *BLEU/ROUGE/perplexity are proxies* #p1 #tag("PITFALL") — Không tự chứng minh factuality, usefulness hoặc safety.
- #chk *Length and domain slices* #p0 #tag("BP") — Eval theo short/long, noisy/clean, domain/source và rare label.
- #chk *Human rubric* #p0 #tag("BP") — Với generation, định nghĩa correctness, completeness, faithfulness, style và unsafe output; sample mù khi so model.
- #chk *Inference decoding config* #p1 #tag("CONFIG") — Greedy/beam/sampling, temperature, top-p và repetition controls là một phần của artifact/eval version.


#callout[*Khi yêu cầu AI code NLP:* cung cấp 10–20 sample thật, label schema, split unit, tokenizer/model checkpoint, max-length policy và metric semantics. Bắt AI báo truncation rate, label alignment example và decoded prediction; đừng chỉ nhận một `Trainer` script chạy được.]


== G. Workflow dùng Coding Agent cho model project


=== G.1 Context packet trước khi yêu cầu code


```text
Problem:
- Business/use-case goal:
- Unit of prediction:
- Task type and expected output:
- Cost of false positive / false negative:

Data contract:
- Sample schema and 10–20 representative samples:
- Label map and ambiguous/empty cases:
- Split rule (group/source/time) and leakage boundary:
- Data/license/privacy constraints:

Model contract:
- Required baseline and allowed pretrained libraries:
- Input/output shapes, dtype, coordinate/token convention:
- Loss, metric, threshold-selection rule:
- Hardware, memory, latency and artifact constraints:

Delivery rules:
1. State assumptions and unresolved decisions before implementation.
2. Build dataset validation + visualization before the full model.
3. Add shape/schema assertions and a one-batch smoke test.
4. Add a tiny-set overfit test and a simple baseline.
5. Separate train, evaluation, inference and export paths.
6. Pin versions; save config, label map and preprocessing with the artifact.
7. Report failures and evidence; do not silently replace requirements.
```


#callout[Prompt tốt không phải là “hãy code YOLO/NLP model hoàn chỉnh”. Prompt tốt đóng băng *contract* nhưng chia implementation thành các checkpoint có thể kiểm tra. Nếu chưa có data/schema/split/metric, hãy yêu cầu AI giúp thiết kế chúng trước, chưa yêu cầu train model.]


=== G.2 Vertical slice để đọc code AI


```mermaid
flowchart TD
    R[Raw sample] --> D[Dataset / transform]
    D --> B[Collate / batch]
    B --> M[Model forward]
    M --> L[Loss + update]
    M --> P[Decode / postprocess]
    P --> E[Metric + error cases]
    P --> I[Inference artifact]
```


+ #chk Đọc một sample thật đi hết flow; ghi schema/shape/dtype/range tại mỗi boundary.
+ #chk Kiểm tra transform nào học statistics, transform nào random và transform nào bắt buộc ở inference.
+ #chk Đối chiếu output head với target encoding và loss API.
+ #chk Đối chiếu decode/postprocess với metric; metric phải nhận đúng coordinate/token/mask semantics.
+ #chk Đối chiếu checkpoint/export với preprocessing, label map, threshold và runtime.


=== G.3 Sáu quality gates — không cho AI nhảy cóc


- #chk *Gate 1 — Data* #p0 #tag("LAB") — Validator chạy; visualize/sample report đúng; duplicates/split/leakage đã kiểm tra.
- #chk *Gate 2 — One batch* #p0 #tag("LAB") — Forward/loss/backward chạy, shapes đúng, loss/grad hữu hạn.
- #chk *Gate 3 — Tiny overfit* #p0 #tag("LAB") — Model nhớ được tiny subset; nếu không, dừng và debug pipeline.
- #chk *Gate 4 — Baseline* #p0 #tag("LAB") — So với naive/simple/pretrained baseline bằng cùng split/metric.
- #chk *Gate 5 — Holdout + slices* #p0 #tag("LAB") — Error taxonomy, per-class/source/length/size metrics và threshold policy.
- #chk *Gate 6 — Inference parity* #p0 #tag("LAB") — Process sạch load artifact; batch/online/export output tương đương trong tolerance.


=== G.4 Phân chia code tay và code với AI


#align(center)[
#table(
  columns: (1fr, 1fr, 1.1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Tự quyết định/viết phần lõi*], [*Để AI tăng tốc*], [*Ưu tiên thư viện đã kiểm chứng*],
  [Problem/data/model/inference contract; split; metric; acceptance tests], [Loader boilerplate, config, CLI, plots, report, tests, refactor, type hints], [Standard preprocessing, pretrained architecture, optimizer/loss API, metric implementation],
  [Một vertical slice nhỏ để hiểu flow], [Mở rộng cùng pattern sang tasks/models/configs], [Architecture production thay vì tự chép paper],
  [Review error case và kết luận], [Tổng hợp logs, group errors, đề xuất ablation], [Export/runtime/operator đã được target platform hỗ trợ],
)
]


- #chk *Ask for explanation after evidence* #p0 #tag("BP") — Yêu cầu AI giải thích bằng sample/shape/metric/log cụ thể, không bằng mô tả chung.
- #chk *Patch, do not regenerate blindly* #p0 #tag("BP") — Khi một gate fail, sửa vùng nhỏ và giữ test regression; regenerate toàn project làm mất dấu nguyên nhân.
- #chk *Version-aware API usage* #p0 #tag("BP") — Cung cấp lockfile/docs đúng version; AI dễ trộn API giữa các release.
- #chk *Security of model artifacts* #p1 #tag("SECURITY") — Không load pickle/checkpoint không tin cậy; kiểm tra nguồn, hash, license và serialization format.


== H. Milestone gọn cho nhánh Model Engineering


#callout[Không làm tất cả. Hoàn thành *H0 + H1*, sau đó chọn *một* trong H2/H3 theo project. Nếu mục tiêu chính vẫn là LLM/RAG/Agent, nhánh này có thể học xen kẽ 20–30% thời gian.]


=== H0 — Tabular ML pipeline (1–2 ngày)


- #chk Một dataset nhỏ có schema, baseline, grouped/stratified/time split đúng semantics.
- #chk `Pipeline`/`ColumnTransformer` tránh leakage; ít nhất hai model family.
- #chk Metric + threshold theo cost FP/FN; confusion matrix và ba error slices.
- #chk Artifact load trong process sạch và inference fixture test.

*Exit evidence:* giải thích được vì sao split/metric/model được chọn, chỉ ra một lỗi
leakage tiềm năng và chứng minh model tốt hơn baseline trên holdout.


=== H1 — PyTorch model pipeline (2–4 ngày)


- #chk Dataset/DataLoader, shape assertions, one-batch smoke test và tiny-set overfit.
- #chk Training/validation tách rõ; `train`/`eval`/inference mode đúng.
- #chk Checkpoint best/last, resume state và reproducible config.
- #chk Error analysis + load-and-infer parity test.

*Exit evidence:* tự trace được sample → batch → logits → loss → prediction → metric;
giải thích được một bug shape/loss/mode mà test đã bắt.


=== H2 — Chọn một CV slice


- #chk Classification, detection hoặc segmentation bằng pretrained model; không tự cài architecture trừ khi bài tập bắt buộc.
- #chk Visualize target sau augmentation; split theo source/scene; metric theo class/size.
- #chk Với detection/segmentation: coordinate/mask contract, empty target và map về ảnh gốc.
- #chk Benchmark preprocessing + postprocessing + model trên target device.

*Exit evidence:* prediction overlay, error taxonomy, per-slice report và artifact contract.
Với bài Road Damage/YOLO, trọng tâm là anchor/assignment/IoU/NMS/mAP và geometry
pipeline; code Darknet/FPN/head có thể để AI hỗ trợ nhưng shape/loss phải tự review.


=== H3 — Chọn một NLP slice


- #chk TF-IDF + linear baseline trước pretrained encoder/LLM.
- #chk Split theo source/author/conversation; kiểm tra duplicate và sequence-length distribution.
- #chk Tokenizer/model pairing, truncation/padding và label alignment nếu NER.
- #chk Macro/micro/entity-level metric đúng task; report theo language/length/domain.

*Exit evidence:* decoded prediction thật, truncation/label-alignment report, error taxonomy
và so sánh baseline với model được chọn theo quality–latency–cost.


=== H4 — Definition of Done cho một model project


- #chk Có problem/data/model/inference contract versioned.
- #chk Có baseline và cùng một eval harness cho mọi candidate.
- #chk Có data validation, shape/schema assertions, tiny-set test và inference parity test.
- #chk Có holdout + slice metrics, threshold policy và human-reviewed error examples.
- #chk Có artifact provenance: code/data/config/dependency/weights/preprocessing/label map.
- #chk Có latency/memory benchmark, monitoring signal, abstain/fallback và rollback plan.
- #chk Có note nêu rõ AI đã sinh phần nào, bạn đã kiểm chứng bằng evidence nào và limitation còn lại.


#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= MỨC 1 — CẦN BIẾT ĐỂ XÂY AI APPLICATION


== 1. LLM Foundations — Mức 1


=== 1.1 Mental model của LLM


```mermaid
flowchart TD
    T[Token sequence] --> A[Attention mixes context]
    A --> R[Context-dependent representations]
    R --> N[Next-token distribution]
    N --> G[Generate one token and repeat]
```


- #chk *Large Language Model (LLM)* #p0 #tag("CONCEPT") — Model sinh chuỗi token dựa trên context, không phải database sự thật.
- #chk *Transformer — overview only* #p0 #tag("CONCEPT") — Architecture nền của phần lớn LLM hiện đại; xử lý token sequence bằng nhiều layer để tạo representation phụ thuộc context.
- #chk *Attention — intuition only* #p0 #tag("CONCEPT") — Cơ chế cho phép representation của một token sử dụng thông tin từ các token liên quan trong context; chưa cần công thức Q/K/V.
- #chk *Context-dependent representation* #p0 #tag("CONCEPT") — Cùng một token có representation khác nhau tùy các token xung quanh, giúp model xử lý nghĩa theo ngữ cảnh.
- #chk *Token* #p0 #tag("CONCEPT") — Đơn vị xử lý; token không đồng nhất với ký tự, từ hoặc byte.
- #chk *Tokenizer* #p0 #tag("CONCEPT") — Quy tắc text ↔ token; tokenizer khác nhau làm thay đổi context/cost.
- #chk *Context window* #p0 #tag("CONCEPT") — Giới hạn tổng input, output và tool messages trong một request.
- #chk *Next-token prediction* #p0 #tag("UTH") — Model ước lượng token kế tiếp, sinh một token rồi lặp lại; đây là mental model đủ dùng cho AI application.
- #chk *Logits* #p1 #tag("UTH") — Điểm chưa chuẩn hóa trước softmax.
- #chk *Probability distribution* #p1 #tag("CONCEPT") — Phân phối xác suất token; không phải xác suất “đúng”.
- #chk *Temperature* #p1 #tag("CONFIG") — Điều chỉnh độ phẳng của phân phối; cao hơn thường đa dạng hơn.
- #chk *Top-p / nucleus sampling* #p1 #tag("CONFIG") — Chỉ lấy tập token có cumulative probability đạt ngưỡng.
- #chk *Max output tokens* #p0 #tag("CONFIG") — Giới hạn output để bảo vệ latency và cost.
- #chk *Reasoning budget / effort* #p1 #tag("CONFIG") — Ngân sách suy luận nếu model/provider hỗ trợ; phải đánh giá cùng quality và latency.
- #chk *Determinism myth* #p1 #tag("PITFALL") — `temperature=0` không bảo đảm byte-for-byte determinism khi backend/model snapshot thay đổi.

=== 1.2 Message và capability


- #chk *System/developer message* #p0 #tag("CONCEPT") — Instruction cấp ứng dụng; không coi nó là rào chắn bảo mật tuyệt đối.
- #chk *User message* #p0 #tag("CONCEPT") — Input không tin cậy, cần validate và rate-limit.
- #chk *Assistant message* #p0 #tag("CONCEPT") — Output của model, có thể chứa text hoặc tool call.
- #chk *Tool message* #p0 #tag("CONCEPT") — Kết quả tool đưa trở lại conversation; phải gắn provenance và giới hạn kích thước.
- #chk *Multimodal input* #p1 #tag("CONCEPT") — Text, image, audio hoặc file; mỗi modality có threat/cost riêng.
- #chk *Streaming* #p1 #tag("API") — Trả partial output; phải xử lý disconnect, incomplete JSON và cancellation.
- #chk *Structured output* #p0 #tag("CONCEPT") — Output tuân JSON Schema/type contract; schema validation vẫn cần ở application boundary.
- #chk *JSON Schema* #p0 #tag("STANDARD") — Mô tả field, type, enum, required và constraint cho output/tool.
- #chk *Tool/function calling* #p0 #tag("CONCEPT") — Model yêu cầu application thực hiện capability bằng arguments có schema.
- #chk *Model capability matrix* #p0 #tag("BP") — Ghi model nào hỗ trợ vision, tools, structured output, streaming, reasoning, batch và context size.
- #chk *Hallucination* #p0 #tag("CONCEPT") — Nội dung tự tin nhưng sai hoặc không có bằng chứng.
- #chk *Grounding* #p0 #tag("CONCEPT") — Ràng buộc câu trả lời vào nguồn, state hoặc tool result có thể kiểm chứng.
- #chk *Abstention / refusal* #p0 #tag("BP") — Từ chối hoặc nói “không đủ dữ liệu” khi confidence/evidence không đạt ngưỡng.

=== 1.3 Request lifecycle và cost


- #chk *Provider API request/response* #p0 #tag("API") — Request ID, model, messages, tools, usage, finish reason và errors.
- #chk *Authentication* #p0 #tag("SECURITY") — API key/service identity không được đưa vào prompt hoặc log.
- #chk *Timeout và deadline* #p0 #tag("CONFIG") — Tách connect timeout, read timeout và end-to-end deadline.
- #chk *Retryable error* #p0 #tag("CONCEPT") — 429, transient 5xx, network reset; retry phải có backoff và idempotency policy.
- #chk *Non-retryable error* #p0 #tag("CONCEPT") — Invalid schema, auth failure, policy refusal; retry mù chỉ làm tăng cost.
- #chk *Token accounting* #p1 #tag("METRIC") — Input/output/cache/tool tokens theo request, tenant và feature.
- #chk *Cost per successful task* #p1 #tag("METRIC") — Cost không chỉ là cost/request; tính cả retry, failed tool và human escalation.
- #chk *Model routing* #p1 #tag("TRADEOFF") — Model rẻ/nhanh cho classify hoặc extract; model mạnh cho synthesis/ambiguous cases.
- #chk *LAB — token meter* #p0 #tag("LAB") — Log token/cost và tạo cảnh báo khi vượt budget của một request.

== 2. Prompt Engineering — Mức 1


=== 2.1 Prompt contract


- #chk *Task statement* #p0 #tag("CONCEPT") — Nói rõ mục tiêu, input, output và tiêu chí thành công.
- #chk *Instruction hierarchy* #p0 #tag("CONCEPT") — Policy/app instructions, user intent, data và tool result có mức tin cậy khác nhau.
- #chk *Role/persona* #p1 #tag("PITFALL") — Persona không thay thế policy, schema hoặc authorization.
- #chk *Input/context/output separation* #p0 #tag("BP") — Dùng delimiter/field rõ để data không bị hiểu như instruction.
- #chk *Output format* #p0 #tag("API") — JSON Schema, enum, Markdown contract hoặc typed object.
- #chk *Few-shot example* #p0 #tag("CONCEPT") — Ví dụ input/output để neo format và edge case.
- #chk *Negative example* #p0 #tag("CONCEPT") — Chỉ ra output không hợp lệ và lý do.
- #chk *Decomposition* #p0 #tag("DP") — Tách classify → retrieve → reason → answer thay vì prompt khổng lồ.
- #chk *Prompt template* #p0 #tag("API") — Template versioned, testable, có placeholder type rõ.
- #chk *Prompt version* #p0 #tag("CONFIG") — Mỗi production trace phải biết prompt version/template hash.

=== 2.2 Kỹ thuật cải thiện chất lượng


- #chk *Zero-shot prompting* #p0 #tag("CONCEPT") — Không ví dụ, phù hợp task đơn giản hoặc model đã quen.
- #chk *Few-shot prompting* #p0 #tag("CONCEPT") — Tăng consistency nhưng tiêu tốn context và có thể truyền bias.
- #chk *Delimiter discipline* #p0 #tag("BP") — Phân cách instructions và untrusted data bằng marker/schema.
- #chk *Question decomposition* #p0 #tag("DP") — Chia câu hỏi phức tạp thành các câu hỏi con có thể kiểm chứng.
- #chk *Query rewriting* #p0 #tag("CONCEPT") — Viết lại query cho retrieval; giữ lại intent và tenant filter.
- #chk *Citation instruction* #p0 #tag("BP") — Yêu cầu dẫn chứng từ source ID, không chỉ “hãy chính xác”.
- #chk *Abstain instruction* #p0 #tag("BP") — Nêu rõ điều kiện không đủ bằng chứng.
- #chk *Self-check* #p1 #tag("CONCEPT") — Model kiểm tra format/claim; không coi self-check là verifier độc lập.
- #chk *Prompt injection* #p0 #tag("SECURITY") — Input/data có thể cố thay đổi instruction hoặc gọi tool trái phép.
- #chk *Prompt leakage* #p0 #tag("SECURITY") — Không đưa secret/policy nội bộ vào prompt nếu không cần.
- #chk *Prompt regression test* #p0 #tag("LAB") — Chạy golden set trước/sau mỗi prompt change.

== 3. Embeddings và Search — Mức 1


=== 3.1 Embedding


- #chk *Embedding model* #p0 #tag("CONCEPT") — Ánh xạ text thành vector để so sánh semantic similarity.
- #chk *Vector dimension* #p0 #tag("CONFIG") — Số chiều cố định theo embedding model; đổi model cần migration/index mới.
- #chk *Cosine similarity* #p0 #tag("CONCEPT") — So sánh hướng vector; normalize và metric phải nhất quán.
- #chk *Dot product / L2 distance* #p0 #tag("CONCEPT") — Metric khác có semantics/index requirement khác.
- #chk *Embedding normalization* #p0 #tag("BP") — Quyết định normalize ở ingest/query và kiểm thử lại ranking.
- #chk *Chunk identity* #p0 #tag("CONCEPT") — `document_id`, version, chunk_id, offsets và source URI để citation/provenance.
- #chk *Metadata* #p0 #tag("CONCEPT") — Tenant, ACL, locale, timestamp, document type, sensitivity và source version.
- #chk *Vector index* #p0 #tag("CONCEPT") — HNSW/IVFFlat hoặc engine tương đương; ANN là trade-off recall/latency.
- #chk *pgvector* #p0 #tag("LIB") — Vector type, similarity operators và index trong PostgreSQL.

=== 3.2 Lexical, hybrid và ranking


- #chk *Lexical search / BM25* #p0 #tag("CONCEPT") — Match từ khóa, tên riêng, mã, số điều khoản.
- #chk *Dense retrieval* #p0 #tag("CONCEPT") — Match ý nghĩa, chịu ảnh hưởng embedding quality.
- #chk *Hybrid retrieval* #p0 #tag("DP") — Kết hợp lexical và dense để giảm blind spot.
- #chk *Top-k* #p0 #tag("CONFIG") — Số candidate; k cao tăng recall/context/cost.
- #chk *Reranker* #p1 #tag("CONCEPT") — Xếp hạng lại candidate bằng cross-encoder/model hoặc rule.
- #chk *Recall\@k* #p1 #tag("METRIC") — Candidate có chứa relevant chunk hay không.
- #chk *MRR / nDCG* #p1 #tag("METRIC") — Đánh giá vị trí và độ quan trọng của relevant result.
- #chk *Tenant/ACL filter* #p0 #tag("SECURITY") — Filter trước hoặc trong retrieval; không lọc sau khi đã đưa dữ liệu vào prompt.
- #chk *LAB — retrieval benchmark* #p0 #tag("LAB") — Dataset query–relevant chunks cố định, đo Recall\@k, MRR và latency p95.

== 4. RAG cơ bản — Mức 1


=== 4.1 Ingestion và generation flow


```mermaid
flowchart LR
    D[Document] --> P[Parse]
    P --> C[Chunk + metadata]
    C --> E[Embed]
    E --> I[Index]
    Q[User query] --> R[Retrieve / rerank]
    I --> R
    R --> X[Context assembly]
    X --> G[Generate + citations]
```


- #chk *Retrieval-Augmented Generation (RAG)* #p0 #tag("CONCEPT") — Retrieve evidence rồi mới generate answer.
- #chk *Document ingestion* #p0 #tag("CONCEPT") — Đồng bộ source, version, deleted/updated state và ACL.
- #chk *Parsing* #p0 #tag("CONCEPT") — Tách text, heading, table, code, footnote, page và layout.
- #chk *Chunking* #p0 #tag("CONCEPT") — Chia tài liệu theo semantic boundary và token budget.
- #chk *Chunk overlap* #p1 #tag("TRADEOFF") — Giữ context giữa chunk nhưng tăng duplicate/cost.
- #chk *Context assembly* #p0 #tag("CONCEPT") — Ghép query, instructions, evidence, source IDs và citation rules.
- #chk *Citation* #p0 #tag("CONCEPT") — Map claim/answer về source/chunk/offset có thể mở lại.
- #chk *Freshness* #p0 #tag("CONCEPT") — Dữ liệu mới/cũ phải có version và policy.
- #chk *Answerability* #p0 #tag("CONCEPT") — Phân biệt “retrieval không tìm thấy” với “tài liệu không có answer”.
- #chk *Stale index* #p1 #tag("PITFALL") — Database source đã đổi nhưng vector index chưa re-embed.
- #chk *Lost-in-the-middle* #p1 #tag("PITFALL") — Evidence giữa context dài có thể bị model bỏ qua.
- #chk *Citation laundering* #p0 #tag("SECURITY") — Model gắn citation thật nhưng claim không được source hỗ trợ.

=== 4.2 RAG correctness contract


- #chk *Groundedness* #p1 #tag("METRIC") — Claim được hỗ trợ bởi context hay không.
- #chk *Citation correctness* #p1 #tag("METRIC") — Citation có đúng source và span không.
- #chk *Retrieval failure taxonomy* #p0 #tag("CONCEPT") — Query lỗi, parser lỗi, chunk lỗi, index lỗi, ACL filter lỗi, reranker lỗi.
- #chk *Abstention threshold* #p0 #tag("CONFIG") — Khi score/coverage thấp thì hỏi lại hoặc từ chối.
- #chk *Prompt injection in documents* #p0 #tag("SECURITY") — Nội dung được retrieve là untrusted data, không phải system instruction.
- #chk *LAB — grounded assistant* #p0 #tag("LAB") — Assistant chỉ trả lời từ 20–50 tài liệu, có source ID, câu “không đủ dữ liệu” và test ACL.

== 5. AI application integration — Mức 1


=== 5.1 Python project foundation


- #chk *Python 3.12+* #p0 #tag("CONFIG") — Chọn version được provider/model dependencies hỗ trợ; không nâng runtime khi chưa chạy test/eval.
- #chk *`pyproject.toml`* #p0 #tag("STANDARD") — Metadata, dependency, build và tool configuration của project.
- #chk *`uv init` / `uv add` / `uv sync`* #p0 #tag("LIB") — Tạo environment và lock dependency có thể tái lập.
- #chk *Virtual environment* #p0 #tag("CONCEPT") — Cô lập interpreter và packages cho từng project.
- #chk *Type hints* #p0 #tag("BP") — Type rõ ở context, state, tool arguments/result và public boundary.
- #chk *Pydantic `BaseModel`* #p0 #tag("LIB") — Validate/serialize typed request, structured output, tool schema và state.
- #chk *`dataclass` / `TypedDict`* #p0 #tag("API") — Model state nội bộ; chọn theo validation/runtime need.
- #chk *`asyncio`* #p0 #tag("CONCEPT") — Concurrency cho I/O model, retrieval và tool; không làm CPU-bound code tự nhanh hơn.
- #chk *`async` / `await`* #p0 #tag("API") — Structured asynchronous flow; không trộn blocking SDK call vào event loop.
- #chk *`httpx.AsyncClient`* #p0 #tag("LIB") — HTTP client có timeout, pooling, cancellation và async support.
- #chk *`pytest` / `pytest-asyncio`* #p0 #tag("LIB") — Unit, integration, async và eval regression tests.
- #chk *Ruff* #p0 #tag("LIB") — Lint/format nhanh, pin rule trong `pyproject.toml`.
- #chk *mypy hoặc Pyright* #p0 #tag("LIB") — Static type checking cho schema/state/tool contracts.
- #chk *Environment variables / secret manager* #p0 #tag("SECURITY") — Config ngoài source; không commit `.env` hoặc đưa secret vào prompt.

=== 5.2 Python AI application track


- #chk *Official provider SDK* #p0 #tag("LIB") — Học request, response, streaming, embeddings và tools trực tiếp trước framework.
- #chk *FastAPI* #p0 #tag("LIB") — Typed HTTP/SSE boundary cho AI service; dependency injection và OpenAPI.
- #chk *Uvicorn* #p0 #tag("LIB") — ASGI server; hiểu worker, timeout, graceful shutdown và event loop.
- #chk *Tokenizer library* #p0 #tag("LIB") — Đếm token, truncate/compact context và ước lượng cost.
- #chk *PostgreSQL + pgvector* #p0 #tag("LIB") — Metadata, state và vector search khi một database boundary là đủ.
- #chk *SQLAlchemy 2.x / psycopg* #p0 #tag("LIB") — Persistence/SQL access; vector search vẫn cần hiểu query plan/index.
- #chk *Redis client* #p0 #tag("LIB") — Cache, rate limit hoặc ephemeral state; không mặc định là source of truth.
- #chk *OpenTelemetry Python* #p0 #tag("LIB") — Trace model, retrieval, tool, HTTP và database flow.
- #chk *LangChain — optional* #p1 #tag("LIB") — Integration components; chỉ thêm sau khi đã tự viết provider → retrieval → prompt → output flow và framework thật sự giảm integration cost.
- #chk *FastAPI vertical slice* #p0 #tag("LAB") — `POST /ask` → context builder → provider SDK → Pydantic output → trace/eval.

=== 5.3 Java/Spring integration track — tùy chọn


- #chk *Integration boundary* #p0 #tag("CONCEPT") — Java giữ auth, business transaction và public API; Python sở hữu AI-specific runtime khi cần tách service.
- #chk *HTTP/gRPC/event contract* #p0 #tag("STANDARD") — Typed request/result, idempotency, deadline, trace context và error code.
- #chk *Spring AI* #p0 #tag("LIB") — Chỉ dùng khi AI feature nằm trực tiếp trong Spring Boot và Python service không tạo lợi ích đủ lớn.
- #chk *Avoid language split by default* #p0 #tag("BP") — Không tách microservice chỉ vì AI dùng Python; tách khi ownership/runtime/scaling/dependency khác thật sự.
- #chk *Contract test* #p0 #tag("LAB") — Kiểm tra Java consumer ↔ Python AI service cho schema, timeout, retry và trace propagation.

=== 5.4 Memory và state


#callout[Đây là vocabulary nhập môn. Học kiến trúc, lifecycle, failure model và lab tại §14.]


- #chk *Conversation history* #p0 #tag("CONCEPT") — Transcript của thread; luôn có giới hạn token/retention.
- #chk *Chat memory* #p0 #tag("CONCEPT") — Strategy chọn message history hoặc summary/retrieval.
- #chk *User/profile memory* #p0 #tag("CONCEPT") — Fact lâu dài có consent, provenance và delete policy.
- #chk *Knowledge base* #p0 #tag("CONCEPT") — Dữ liệu truy hồi; không tự động là memory cá nhân.
- #chk *Session state* #p0 #tag("CONCEPT") — State của request/workflow; cần schema và TTL.
- #chk *Memory poisoning* #p0 #tag("SECURITY") — User/model ghi fact sai hoặc độc hại vào memory tương lai.

== 6. Tool/function calling — Mức 1


- #chk *Tool schema* #p0 #tag("API") — Tên, mô tả, JSON Schema arguments và output envelope.
- #chk *Tool selection* #p0 #tag("CONCEPT") — Model chọn tool; application vẫn phải authorize.
- #chk *Tool execution boundary* #p0 #tag("SECURITY") — Model không trực tiếp có database/network privilege.
- #chk *Read-only tool* #p0 #tag("BP") — Bắt đầu bằng search/lookup trước khi expose mutation.
- #chk *Idempotent tool* #p0 #tag("BP") — Retry cùng idempotency key không nhân side effect.
- #chk *Dry-run / preview* #p0 #tag("DP") — Preview thay đổi trước approval/commit.
- #chk *Tool result envelope* #p0 #tag("API") — `ok`, `data`, `error_code`, `retryable`, `provenance`, `request_id`.
- #chk *Tool timeout* #p0 #tag("CONFIG") — Tool chậm không được treo toàn bộ agent.
- #chk *Argument validation* #p0 #tag("SECURITY") — Validate type, range, ownership, allowlist và business invariant.
- #chk *Tool output injection* #p0 #tag("SECURITY") — Kết quả tool có thể chứa text độc hại; giữ data/instruction boundary.
- #chk *LAB — safe calculator/search tool* #p0 #tag("LAB") — Schema strict, read-only, timeout, audit log và test malformed args.

== 7. Evaluation và testing — Mức 1


- #chk *Golden set* #p0 #tag("CONCEPT") — Tập input/output/evidence cố định theo product use case.
- #chk *Task success metric* #p1 #tag("METRIC") — Định nghĩa “đúng” bằng outcome, không chỉ câu trả lời nghe hay.
- #chk *Exact-match test* #p0 #tag("CONCEPT") — Phù hợp enum/JSON/SQL nhỏ; không đủ cho open-ended answer.
- #chk *Semantic similarity* #p1 #tag("METRIC") — So sánh ý nghĩa; có thể bỏ sót factual error.
- #chk *LLM-as-judge* #p1 #tag("TRADEOFF") — Chấm rubric nhanh nhưng có bias, variance và judge drift.
- #chk *Groundedness/citation eval* #p1 #tag("METRIC") — Kiểm tra claim–evidence.
- #chk *Tool success eval* #p1 #tag("METRIC") — Đúng tool, args, side effect và error handling.
- #chk *Regression eval* #p0 #tag("BP") — Chạy trước/sau model, prompt, chunker hoặc retrieval change.
- #chk *Human review sample* #p1 #tag("BP") — Calibration cho judge và phát hiện failure mới.
- #chk *Latency/cost eval* #p1 #tag("METRIC") — p50/p95, time-to-first-token, token và cost per task.
- #chk *LAB — eval gate* #p0 #tag("LAB") — CI fail nếu quality tụt quá ngưỡng hoặc cost/latency vượt budget.

== 8. AI security căn bản — Mức 1


#callout[Đây là baseline security. Permission model, risk tier và guardrail architecture nằm tại §16.]


- #chk *Direct prompt injection* #p0 #tag("SECURITY") — User cố đổi policy/role.
- #chk *Indirect prompt injection* #p0 #tag("SECURITY") — Document, webpage, email hoặc tool result chứa instruction độc.
- #chk *Sensitive information disclosure* #p0 #tag("SECURITY") — Leak PII, secret, system prompt, tenant data.
- #chk *Improper output handling* #p0 #tag("SECURITY") — Render output như HTML/SQL/code mà không validate/escape.
- #chk *Least privilege* #p0 #tag("BP") — Mỗi tool chỉ có quyền tối thiểu theo user/tenant/use case.
- #chk *Allowlist* #p0 #tag("SECURITY") — Domain, table, operation, file path, enum và recipient phải được giới hạn.
- #chk *Human approval* #p1 #tag("DP") — Pause trước payment, delete, send message, legal/medical/high-impact action.
- #chk *Audit trail* #p0 #tag("CONCEPT") — Actor, model, prompt version, tool, args hash, approval và outcome.
- #chk *Secret redaction* #p0 #tag("BP") — Không ghi API key, token, PII hoặc raw prompt nhạy cảm vào log mặc định.
- #chk *Threat model* #p1 #tag("DP") — Xác định asset, actor, trust boundary, data flow và abuse case trước feature mới.
- #chk *M1 Definition of Done* #p0 #tag("BP") — Có input validation, output schema, source/citation, timeout, budget, eval set,
trace ID, permission check và một test prompt-injection.

#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= MỨC 2 — BASIC AGENT + PRODUCTION AI


== 9. Production LLM runtime


- #chk *Provider adapter* #p1 #tag("DP") — Cô lập provider-specific request, error và usage khỏi domain code.
- #chk *Model registry* #p1 #tag("CONCEPT") — Model ID, snapshot, capability, price, context và approval status.
- #chk *Model routing policy* #p1 #tag("DP") — Route theo task, tenant, latency, risk, language và budget.
- #chk *Fallback model* #p1 #tag("TRADEOFF") — Tăng availability nhưng có thể đổi quality/schema/tool behavior.
- #chk *Deadline propagation* #p1 #tag("BP") — Request deadline phải truyền xuống model, retriever và tool.
- #chk *Retry budget* #p1 #tag("CONFIG") — Số retry và tổng thời gian cố định; không retry trong mọi layer cùng lúc.
- #chk *Exponential backoff + jitter* #p1 #tag("DP") — Tránh retry storm trong 429/5xx.
- #chk *Circuit breaker* #p1 #tag("DP") — Ngắt provider lỗi liên tục, mở lại bằng probe.
- #chk *Rate limiting* #p1 #tag("CONFIG") — Limit theo user, tenant, API key, model và tool cost.
- #chk *Concurrency limit* #p1 #tag("CONFIG") — Hạn chế in-flight LLM/tool calls để bảo vệ pool.
- #chk *Token budget* #p1 #tag("CONFIG") — Ngân sách input, output, tool calls và total run.
- #chk *Cost budget* #p1 #tag("CONFIG") — Hard stop, soft warning và escalation khi vượt tiền.
- #chk *Prompt/result cache* #p0 #tag("TRADEOFF") — Giảm cost/latency nhưng phải tính freshness, tenant isolation và privacy.
- #chk *Semantic cache* #p0 #tag("TRADEOFF") — Cache theo similarity; dễ trả answer sai cho query gần nhưng khác intent.
- #chk *Batching* #p1 #tag("TRADEOFF") — Tăng throughput, đổi lại latency per item và complexity.
- #chk *Cancellation* #p1 #tag("API") — Client disconnect hoặc deadline phải hủy stream/tool nếu provider hỗ trợ.
- #chk *Idempotency key* #p1 #tag("API") — Bắt buộc cho mutation tool và async job.
- #chk *Backpressure* #p1 #tag("CONCEPT") — Queue/buffer phải có giới hạn và chính sách drop/dead-letter.
- #chk *Load shedding* #p1 #tag("DP") — Từ chối task không quan trọng khi capacity thiếu.
- #chk *LAB — provider outage* #p1 #tag("LAB") — Mô phỏng 429, timeout, malformed output và provider down; kiểm tra fallback, breaker, budget.

== 10. Context Engineering


=== 10.1 Context là runtime resource


- #chk *Context budget* #p1 #tag("CONCEPT") — Tổng token, latency, cost và attention budget.
- #chk *Context composition* #p1 #tag("CONCEPT") — Policy, task, user input, memory, retrieved evidence, tool result và output schema.
- #chk *Instruction/data boundary* #p1 #tag("SECURITY") — Dữ liệu retrieve/tool không được tự nâng quyền thành instruction.
- #chk *Relevance policy* #p1 #tag("BP") — Chọn context theo task và evidence, không nhồi mọi thứ.
- #chk *Recency policy* #p1 #tag("CONCEPT") — Dữ liệu mới có thể ưu tiên hơn nhưng cần kiểm tra authority.
- #chk *Authority/provenance* #p1 #tag("CONCEPT") — Source đáng tin, version, owner và timestamp.
- #chk *Context compression* #p1 #tag("DP") — Summarize, extract facts hoặc giảm field trước khi gửi model.
- #chk *Compaction boundary* #p1 #tag("CONCEPT") — Compact history khi đạt ngưỡng; giữ invariant và open tasks.
- #chk *Lost-in-the-middle mitigation* #p1 #tag("BP") — Đưa evidence quan trọng lên vị trí ổn định, rerank, giảm context thừa.
- #chk *Context window overflow* #p0 #tag("PITFALL") — Truncation vô tình xóa policy, user constraint hoặc tool result.
- #chk *Context poisoning* #p1 #tag("SECURITY") — Một nguồn độc làm sai các turn/workflow sau.
- #chk *Tenant isolation* #p1 #tag("SECURITY") — Context cache/memory/retrieval không được cross-tenant.
- #chk *PII minimization* #p1 #tag("SECURITY") — Chỉ đưa field cần cho task, redact phần còn lại.

=== 10.2 Memory policy


- #chk *Read memory policy* #p1 #tag("CONCEPT") — Khi nào đọc memory, loại fact nào được inject.
- #chk *Write memory policy* #p1 #tag("CONCEPT") — Ai được ghi, confidence, consent, TTL, provenance và delete.
- #chk *Summary memory* #p1 #tag("TRADEOFF") — Rẻ context nhưng mất chi tiết và có thể biến sai lầm thành “fact”.
- #chk *Episodic memory* #p1 #tag("CONCEPT") — Sự kiện theo thời gian, useful cho workflow resume.
- #chk *Semantic memory* #p1 #tag("CONCEPT") — Fact/knowledge trích xuất, cần nguồn và confidence.
- #chk *Working memory* #p1 #tag("CONCEPT") — Scratch state ngắn hạn của một run; không tự lưu vĩnh viễn.
- #chk *Memory conflict resolution* #p1 #tag("CONCEPT") — Fact mới/cũ, source authority và human correction.
- #chk *LAB — context profiler* #p1 #tag("LAB") — Hiển thị token/latency/quality khi thêm/bớt từng context source.

== 11. RAG nâng cao


- #chk *Layout-aware parsing* #p1 #tag("CONCEPT") — Giữ heading, table, list, page, code và tọa độ.
- #chk *Semantic chunking* #p1 #tag("CONCEPT") — Chunk theo cấu trúc/ý nghĩa thay vì độ dài cố định.
- #chk *Parent-child retrieval* #p0 #tag("DP") — Retrieve child nhỏ, đưa parent vừa đủ vào context.
- #chk *Multi-vector representation* #p1 #tag("CONCEPT") — Một document có vector title, body, table hoặc summary.
- #chk *Query expansion* #p1 #tag("CONCEPT") — Tạo synonym/sub-query; cần giữ tenant/filter/intent.
- #chk *Query decomposition* #p1 #tag("DP") — Multi-hop retrieval cho câu hỏi có nhiều điều kiện.
- #chk *HyDE* #p1 #tag("CONCEPT") — Sinh hypothetical answer để tạo query embedding; phải eval hallucination/latency.
- #chk *Hybrid score fusion* #p1 #tag("CONCEPT") — Reciprocal rank fusion hoặc weighted score; normalize trước khi trộn.
- #chk *Reranking window* #p1 #tag("CONFIG") — Candidate nhiều hơn top-k final; đo cost/latency.
- #chk *Metadata/security filter first* #p0 #tag("SECURITY") — ACL/tenant/time filter trước khi ranking và context assembly.
- #chk *Freshness/CDC ingestion* #p1 #tag("CONCEPT") — Đồng bộ update/delete, tombstone và re-embed theo version.
- #chk *Citation span* #p1 #tag("CONCEPT") — Page/line/offset hoặc source fragment đủ để audit.
- #chk *Answer coverage* #p1 #tag("METRIC") — Tất cả claim quan trọng có evidence tương ứng.
- #chk *Retrieval ablation* #p0 #tag("LAB") — So sánh lexical, dense, hybrid, rerank và no-RAG trên cùng golden set.

== 12. Tool Engineering


=== 12.1 Thiết kế tool


- #chk *Capability boundary* #p1 #tag("CONCEPT") — Tool là capability hẹp, không phải “god function”.
- #chk *Stable name/description* #p1 #tag("API") — Mô tả khi nào dùng, khi nào không dùng, side effect và permission.
- #chk *Typed arguments* #p1 #tag("API") — Enum/range/format/required; reject unknown field nếu phù hợp.
- #chk *Typed result* #p1 #tag("API") — Stable envelope, pagination, source/provenance và machine-readable errors.
- #chk *Deterministic core* #p1 #tag("BP") — Business rule và authorization nằm trong code/service, không giao cho model.
- #chk *Idempotent mutation* #p1 #tag("BP") — Idempotency key, dedupe record và retry policy.
- #chk *Dry-run/preview* #p1 #tag("DP") — Trả diff/plan trước commit.
- #chk *Two-phase action* #p1 #tag("DP") — Prepare → approve → commit cho side effect nguy hiểm.
- #chk *Capability token* #p1 #tag("SECURITY") — Token scoped theo user, tenant, action, TTL và audience.
- #chk *SSRF protection* #p1 #tag("SECURITY") — Egress allowlist, DNS/IP validation, block metadata/private ranges, redirect policy.
- #chk *Sandbox* #p1 #tag("SECURITY") — Cô lập code execution/file/browser tool.
- #chk *Tool audit* #p1 #tag("METRIC") — Invocation, arguments hash, latency, outcome, policy decision và actor.

=== 12.2 Tool failure và correction


- #chk *Validation error* #p0 #tag("CONCEPT") — Không retry nguyên xi; trả lỗi có thể sửa.
- #chk *Transient tool error* #p1 #tag("CONCEPT") — Retry bounded với backoff.
- #chk *Permanent business error* #p1 #tag("CONCEPT") — Dừng hoặc hỏi user; không loop.
- #chk *Partial side effect* #p1 #tag("PITFALL") — Timeout sau khi remote đã commit; cần idempotency/status query.
- #chk *Compensation tool* #p1 #tag("DP") — Undo/compensate khi transaction xuyên service không atomic.
- #chk *Tool result verification* #p1 #tag("BP") — Kiểm tra schema, ownership, freshness, invariant trước khi đưa vào context.
- #chk *Tool output size cap* #p1 #tag("CONFIG") — Tránh tool response làm tràn context.
- #chk *LAB — mutation gate* #p1 #tag("LAB") — Tool tạo “draft invoice/email” trước, approval mới gửi/commit; test double-submit và timeout.

== 13. Basic Agent Engineering — Mức 2


```mermaid
flowchart LR
    O[Observe state] --> D[Decide next action]
    D --> A[Act: model / tool / human]
    A --> V[Verify result]
    V --> S{Stop?}
    S -- No --> O
    S -- Yes --> F[Final answer / artifact]
```


- #chk *Agent loop* #p0 #tag("CONCEPT") — Observe → decide → act → verify → repeat.
- #chk *Manual bounded loop first* #p0 #tag("BP") — Viết loop/state/stop condition trực tiếp trước để hiểu semantics; framework không thay thế termination, authorization hoặc eval.
- #chk *LangGraph — optional ở Mức 2* #p1 #tag("LIB") — Chỉ thêm khi basic agent cần stateful loop/branch/interrupt; persistence, replay và graph migration sâu thuộc Mức 3.
- #chk *State snapshot* #p0 #tag("CONCEPT") — Input, plan, evidence, tool results, errors, budget và pending approvals.
- #chk *Stop condition* #p0 #tag("CONCEPT") — Goal reached, answerable, blocked, budget/deadline exceeded hoặc user cancels.
- #chk *Max steps* #p0 #tag("CONFIG") — Hard cap để ngăn loop vô hạn.
- #chk *Max tool calls* #p0 #tag("CONFIG") — Cap theo tool/risk/tenant.
- #chk *Deadline/cost guard* #p0 #tag("CONFIG") — Loop dừng khi không còn time/money.
- #chk *Progress invariant* #p0 #tag("BP") — Mỗi iteration phải thay đổi state hoặc tạo evidence mới.
- #chk *Retry vs replan* #p0 #tag("TRADEOFF") — Retry cùng action cho transient error; replan khi premise/goal đổi.
- #chk *Reflection/critic* #p1 #tag("TRADEOFF") — Có thể tăng quality nhưng tăng latency/cost và tự phê bình không độc lập.
- #chk *Plan–execute* #p1 #tag("DP") — Tách lập kế hoạch và thực thi; plan stale khi state thay đổi.
- #chk *Human-in-the-loop* #p1 #tag("DP") — Pause khi uncertainty/risk vượt ngưỡng.
- #chk *Oscillation detection* #p1 #tag("CONCEPT") — Phát hiện lặp tool/query/state không tiến triển.
- #chk *Escalation* #p0 #tag("CONCEPT") — Chuyển người hoặc workflow fallback với đủ context và lý do.
- #chk *LAB — bounded research agent* #p0 #tag("LAB") — Agent tìm tài liệu với 5 bước, 30 giây, 2 đô-la, citations và stop reason.

== 14. Memory Architecture, state và persistence cho agent


#callout[*Điểm học chính của Memory Architecture.* §5.4 giới thiệu các loại memory, §10.2 mô tả read/write policy; phần này ghép chúng thành kiến trúc hoàn chỉnh. Memory không phải “nhét lại toàn bộ hội thoại”. Nó là dữ liệu có vòng đời, ownership, provenance, confidence và quyền truy cập rõ ràng.]


=== 14.1 Phân biệt state, history, knowledge và memory


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Thành phần*], [*Phạm vi*], [*Ví dụ*], [*Nơi lưu phù hợp*], [*Rủi ro chính*],
  [Conversation history], [Một thread], [User/assistant/tool messages], [Message store có retention], [Context phình to, giữ dữ liệu thừa],
  [Working memory], [Một run hoặc task], [Plan, evidence, pending tool calls], [Typed runtime state/checkpoint], [State stale hoặc merge conflict],
  [Episodic memory], [Nhiều run theo thời gian], [“User đã sửa cấu hình X hôm qua”], [Event/time-indexed store], [Sự kiện cũ bị hiểu như fact hiện tại],
  [Semantic memory], [Fact tương đối ổn định], [Preference, profile, extracted fact], [Structured store hoặc vector index], [Fact sai, conflict, thiếu provenance],
  [Knowledge base], [Dữ liệu dùng chung], [Tài liệu sản phẩm, policy], [Document store + retrieval index], [Stale index, ACL leakage],
  [Durable workflow state], [Một execution dài], [Node hiện tại, approval đang chờ], [Checkpointer/database], [Resume lặp side effect],
)
]


- #chk *Thread state* #p0 #tag("CONCEPT") — State chỉ của một conversation/run.
- #chk *Durable workflow state* #p0 #tag("CONCEPT") — State phải survive process crash/deploy.
- #chk *Checkpoint* #p1 #tag("CONCEPT") — Snapshot state ở boundary có thể resume.
- #chk *Event log* #p1 #tag("CONCEPT") — Append-only events để audit/replay; không luôn thay checkpoint.
- #chk *Checkpointer* #p1 #tag("API") — Lưu graph/run state theo thread hoặc execution ID.
- #chk *Long-term store* #p1 #tag("CONCEPT") — Store fact/memory khác với checkpointer.
- #chk *State schema* #p0 #tag("API") — Typed fields, version, defaults, migration policy.
- #chk *Resume token* #p1 #tag("API") — Định danh run/interrupt để tiếp tục đúng execution.
- #chk *Exactly-once illusion* #p1 #tag("PITFALL") — Resume/retry có thể chạy side effect lặp; thiết kế idempotency/dedupe.
- #chk *State migration* #p0 #tag("BP") — Graph/code version mới phải đọc được checkpoint cũ hoặc fail rõ ràng.
- #chk *Retention/delete* #p1 #tag("SECURITY") — TTL, legal hold, user delete và tenant isolation.

=== 14.2 Memory lifecycle


```mermaid
flowchart TD
    O[Observe candidate fact] --> V[Validate and classify]
    V --> C{Consent and policy?}
    C -- No --> X[Use only in current run]
    C -- Yes --> W[Write with provenance]
    W --> R[Retrieve by task and ACL]
    R --> K[Resolve conflict and freshness]
    K --> U[Use in context]
    U --> E[Expire, correct, or delete]
```


- #chk *Candidate extraction* #p1 #tag("CONCEPT") — Model có thể đề xuất fact; application quyết định có ghi hay không.
- #chk *Memory classification* #p1 #tag("BP") — Phân loại preference, identity, task fact, episode, sensitive data và derived inference.
- #chk *Write gate* #p1 #tag("SECURITY") — Kiểm tra consent, data class, source authority, confidence, TTL và tenant trước khi persist.
- #chk *Provenance record* #p1 #tag("API") — `source`, `observed_at`, `written_by`, `confidence`, `scope`, `expires_at`, `version`.
- #chk *Read gate* #p1 #tag("SECURITY") — Truy hồi theo task relevance, actor, purpose, tenant và sensitivity; tìm thấy không đồng nghĩa được inject.
- #chk *Conflict resolution* #p1 #tag("CONCEPT") — Ưu tiên explicit user correction, authoritative source, freshness và confidence.
- #chk *Memory consolidation* #p1 #tag("TRADEOFF") — Gộp nhiều episode thành fact giúp giảm noise nhưng có thể biến suy luận thành sự thật.
- #chk *Correction/delete propagation* #p1 #tag("BP") — Sửa source of truth, cache, vector index và derived summary; không chỉ xóa một message.
- #chk *Data minimization* #p1 #tag("SECURITY") — Chỉ lưu field cần thiết; tránh lưu raw transcript khi structured fact đã đủ.

=== 14.3 Kiến trúc đọc và ghi memory


- #chk *Memory writer* #p1 #tag("CONCEPT") — Pipeline extract → validate → authorize → dedupe → persist; tách khỏi model response path khi phù hợp.
- #chk *Memory retriever* #p1 #tag("CONCEPT") — Filter ACL/time/type trước, sau đó lexical/vector rank và relevance threshold.
- #chk *Memory resolver* #p1 #tag("CONCEPT") — Hợp nhất duplicate/conflict và trả fact kèm provenance, không trả text vô nguồn.
- #chk *Context injector* #p1 #tag("CONCEPT") — Chuyển memory đã duyệt thành context với label “data”, token budget và priority.
- #chk *Memory feedback* #p1 #tag("API") — Cho user xem, sửa, quên hoặc giới hạn phạm vi sử dụng memory.
- #chk *Cache vs memory* #p0 #tag("TRADEOFF") — Cache tối ưu hiệu năng và có thể bỏ; memory mang semantics sản phẩm và cần lifecycle.
- #chk *Vector store is not memory architecture* #p1 #tag("PITFALL") — Vector DB chỉ là một retrieval mechanism, không giải quyết consent, conflict, ownership hay deletion.

=== 14.4 Memory failure matrix


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Failure*], [*Dấu hiệu*], [*Control*],
  [Memory poisoning], [Fact lạ xuất hiện sau tool/document độc], [Source trust, write gate, quarantine, human correction],
  [Cross-tenant recall], [Fact của tenant khác vào context], [Partition key + ACL filter trước ranking + isolation test],
  [Stale preference], [Agent dùng lựa chọn user đã đổi], [Version/freshness, explicit correction priority],
  [False consolidation], [Summary biến inference thành fact], [Preserve evidence, confidence, reversible derivation],
  [Unbounded growth], [Latency/token/storage tăng theo thời gian], [TTL, dedupe, compaction, retention budget],
  [Incomplete deletion], [Fact đã xóa vẫn xuất hiện], [Tombstone, cache/index invalidation, deletion audit],
  [Resume duplicate action], [Workflow gửi/ghi hai lần], [Idempotency key, status query, checkpoint boundary],
)
]


- #chk *LAB — memory lifecycle* #p1 #tag("LAB") — Xây typed memory service có consent, provenance,
conflict resolution, TTL và delete; test poisoning, stale fact, cross-tenant retrieval
và việc xóa đồng bộ source/cache/index.

== 15. Observability và evaluation trong production


- #chk *Trace* #p1 #tag("CONCEPT") — Một request/run xuyên model, retrieval, tool, graph node và human step.
- #chk *Span* #p1 #tag("CONCEPT") — Đơn vị timing/attributes cho model call, tool, DB, reranker.
- #chk *Trace ID / correlation ID* #p1 #tag("API") — Nối API log với agent run và downstream systems.
- #chk *Prompt/model version tagging* #p0 #tag("BP") — Không thể debug regression nếu thiếu version.
- #chk *Token/cost metrics* #p1 #tag("METRIC") — Input/output/cache/reasoning/tool token.
- #chk *TTFT / end-to-end latency* #p1 #tag("METRIC") — Time-to-first-token và completion time.
- #chk *Tool latency/error rate* #p1 #tag("METRIC") — Theo tool, tenant, error class.
- #chk *Retrieval metrics* #p0 #tag("METRIC") — Recall\@k/MRR/off-topic rate/filter rejection.
- #chk *Quality metrics* #p1 #tag("METRIC") — Task success, groundedness, citation correctness, refusal precision.
- #chk *Online feedback* #p1 #tag("CONCEPT") — User correction, retry, escalation, abandonment; beware selection bias.
- #chk *Trace sampling/redaction* #p1 #tag("SECURITY") — Prompt/completion có thể chứa PII/secrets; không bật raw logging mặc định.
- #chk *Offline eval gate* #p1 #tag("BP") — Golden set chạy trước deploy/model/prompt/index change.
- #chk *Canary/shadow* #p1 #tag("DP") — Model/prompt mới nhận traffic giới hạn hoặc chỉ quan sát.
- #chk *Rollback bundle* #p1 #tag("BP") — Model ID, prompt, retrieval config, tool schema, index version và feature flags.
- #chk *LAB — trace-to-eval* #p1 #tag("LAB") — Chọn 100 production traces đã redacted, biến thành regression cases, link trace → failure taxonomy.

=== 15.1 Evaluation validity và dataset lifecycle


#callout[Eval chỉ hữu ích khi case, rubric, judge và release decision đều có provenance. Một con số trung bình cao có thể che failure nghiêm trọng ở slice hiếm hoặc chỉ phản ánh đúng dữ liệu mà team đã tối ưu lặp lại quá nhiều lần.]


- #chk *Eval-case contract* #p0 #tag("API") — Mỗi case có input, expected outcome/evidence, rubric, tags/slice, source, owner và version; open-ended task không ép thành exact string nếu semantics mới là điều cần đo.
- #chk *Slice coverage* #p0 #tag("METRIC") — Báo cáo theo task, language, tenant/risk tier, input length, source và known failure class; aggregate score không được che slice dưới ngưỡng.
- #chk *Eval contamination / leakage* #p0 #tag("PITFALL") — Case hoặc near-duplicate lọt vào prompt examples, tuning data hay quá trình chọn model làm kết quả lạc quan; giữ holdout và provenance đủ để phát hiện overlap.
- #chk *Rubric và adjudication* #p1 #tag("BP") — Rubric có tiêu chí/pass threshold và ví dụ biên; disagreement giữa annotators/judges được review, không âm thầm lấy một nhãn bất kỳ làm truth.
- #chk *Judge calibration* #p1 #tag("BP") — So LLM judge với human-labeled sample theo từng rubric/slice, pin judge prompt/model và theo dõi drift; judge không được tự chấm chính output của mình mà không có control độc lập.
- #chk *Repeated trials / variance* #p1 #tag("METRIC") — Với output không deterministic, chạy nhiều seed/sample khi cần và báo pass rate/variance; một lần chạy pass không chứng minh behavior ổn định.
- #chk *Paired comparison và uncertainty* #p1 #tag("METRIC") — So candidate với baseline trên cùng cases, xem số case win/loss và confidence/uncertainty phù hợp; chênh lệch rất nhỏ không tự là lý do deploy.
- #chk *Release threshold theo risk* #p0 #tag("BP") — Có non-regression gate tổng thể, hard gate cho security/high-risk slice và budget latency/cost; nêu rõ ai được override và rollback trigger.
- #chk *Feedback is not ground truth* #p0 #tag("PITFALL") — Thumbs-up, retry, abandonment và complaint chịu selection/context bias; chỉ trở thành label sau sampling, review và rubric phù hợp.
- #chk *Trace → candidate case → labeled case* #p1 #tag("BP") — Giữ lineage, consent/redaction, dedupe, failure taxonomy và split policy khi biến production trace thành eval/training data; tránh đưa test holdout trở lại optimization loop.
- #chk *Eval-set lifecycle* #p1 #tag("BP") — Version thêm/sửa/xóa case, giữ historical comparability, refresh theo production failures nhưng không thay benchmark sau khi thấy kết quả chỉ để candidate được pass.

#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Failure*], [*Dấu hiệu*], [*Control*],
  [Average che slice nguy hiểm], [Overall tăng nhưng refusal/ACL hoặc ngôn ngữ hiếm giảm], [Slice gate + minimum sample + risk-weighted review],
  [Judge drift], [Cùng output đổi điểm sau khi thay judge], [Pin version, calibration sample, paired re-score],
  [Eval overfitting], [Golden score tăng nhưng production complaint không giảm], [Hidden holdout, fresh traces, online shadow/canary],
  [Feedback bias], [Chỉ user rất hài lòng/rất bực mới gửi feedback], [Sampling có chủ đích + human labeling + provenance],
  [Benchmark bị sửa sau kết quả], [Candidate “pass” nhờ đổi case/rubric], [Immutable run manifest + reviewed dataset version],
)
]


- #chk *LAB — trustworthy eval change* #p1 #tag("LAB") — So baseline/candidate trên versioned cases,
report theo slice và repeated trials, calibrate judge bằng human sample, ghi release
decision cùng threshold/override/rollback rồi thêm production failure mới mà không làm rò holdout.

== 16. Guardrails, permissions, AI security và governance — Mức 2


#callout[*Điểm học chính của Guardrails & Permissions.* §8 là baseline; phần này tổ chức guardrail theo trust boundary. Guardrail không phải một prompt “hãy an toàn”, mà là nhiều lớp kiểm soát trước, trong và sau model/tool execution.]


=== 16.1 Guardrail architecture


```mermaid
flowchart TD
    I[Input and identity] --> P[Policy decision]
    P --> M[Model and context]
    M --> T{Tool requested?}
    T -- Yes --> A[Authorize and validate]
    A --> X[Execute in sandbox]
    T -- No --> O[Validate output]
    X --> O
    O --> D[Deliver or escalate]
```


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Lớp*], [*Kiểm soát chính*], [*Không được thay thế bởi*],
  [Input], [Authentication, classification, size/rate limit], [Prompt instruction],
  [Context], [ACL, provenance, instruction/data boundary], [Model tự nhận biết dữ liệu độc],
  [Model], [Allowed models, structured output, policy routing], [Business authorization],
  [Tool request], [Schema, ownership, scope, risk classification], [Model “xin phép” bằng text],
  [Execution], [Sandbox, scoped credential, egress/filesystem limits], [Output filtering],
  [Output], [Schema, factual/policy checks, encoding/redaction], [Chỉ LLM-as-judge],
  [Side effect], [Preview, approval, idempotency, audit], [Chat confirmation mơ hồ],
)
]


- #chk *Guardrail* #p1 #tag("CONCEPT") — Control kỹ thuật hoặc policy làm giảm khả năng/hậu quả của hành vi sai.
- #chk *Preventive control* #p1 #tag("CONCEPT") — Chặn trước execution: deny, allowlist, schema, scope, sandbox.
- #chk *Detective control* #p1 #tag("CONCEPT") — Phát hiện trong/sau run: anomaly, audit, trace, policy violation.
- #chk *Corrective control* #p1 #tag("CONCEPT") — Rollback, compensation, revoke credential, quarantine memory/index.
- #chk *Deterministic guardrail first* #p1 #tag("BP") — Dùng type/rule/ACL/business invariant khi có thể; model judge dành cho semantics khó mã hóa.
- #chk *Fail closed vs fail safe* #p1 #tag("TRADEOFF") — Action nguy hiểm fail closed; tác vụ ít rủi ro có thể trả partial/fallback an toàn.

=== 16.2 Permission model


- #chk *Principal* #p1 #tag("CONCEPT") — User, service, agent hoặc worker đang yêu cầu capability.
- #chk *Resource* #p1 #tag("CONCEPT") — Record, tenant data, file, mailbox, payment hoặc external system bị tác động.
- #chk *Action* #p1 #tag("CONCEPT") — Read, search, draft, send, update, delete, execute hoặc delegate.
- #chk *Contextual condition* #p1 #tag("CONCEPT") — Tenant, environment, risk, time, purpose, approval và data classification.
- #chk *RBAC* #p1 #tag("TRADEOFF") — Role dễ quản lý nhưng thường quá thô cho agent action.
- #chk *ABAC* #p1 #tag("TRADEOFF") — Policy theo attributes linh hoạt hơn nhưng khó debug và test.
- #chk *Capability-based permission* #p1 #tag("DP") — Trao token/quyền hẹp cho đúng action, resource, audience và TTL.
- #chk *Delegated authority* #p1 #tag("SECURITY") — Worker không được nhận nhiều quyền hơn delegator; handoff phải giữ scope.
- #chk *Permission check at execution* #p1 #tag("BP") — Re-check ngay trước side effect, không dựa vào quyền lúc prompt/context được tạo.
- #chk *Confused deputy* #p1 #tag("SECURITY") — Agent có đặc quyền bị user/tool data lợi dụng để hành động thay kẻ không có quyền.
- #chk *Revocation* #p1 #tag("CONCEPT") — Quyền/approval có thể hết hạn hoặc bị thu hồi trước commit.

=== 16.3 Risk-tiered action policy


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Risk*], [*Ví dụ*], [*Mặc định*],
  [R0 — Read public], [Đọc tài liệu công khai], [Cho phép, log tối thiểu],
  [R1 — Read private], [Đọc lịch/file thuộc user], [ACL + purpose + audit],
  [R2 — Reversible mutation], [Tạo draft, thêm label], [Preview hoặc thông báo; idempotency],
  [R3 — External side effect], [Gửi email, publish, đặt lịch], [Exact target + explicit approval],
  [R4 — Irreversible/high impact], [Xóa, thanh toán, quyết định pháp lý/y tế], [Strong verification + human owner; có thể cấm autonomy],
)
]


- #chk *Action risk classifier* #p1 #tag("CONCEPT") — Rule-based trước; model chỉ hỗ trợ khi semantics mơ hồ.
- #chk *Approval binding* #p1 #tag("SECURITY") — Approval phải gắn actor, exact action, target, diff/amount, expiry và idempotency key.
- #chk *Policy decision record* #p1 #tag("API") — Lưu `allow/deny/require_approval`, policy version, reason và evaluated attributes.
- #chk *Blast-radius limit* #p1 #tag("BP") — Cap số record, tiền, recipient, file và thời gian trong một run.
- #chk *Break-glass path* #p1 #tag("SECURITY") — Quy trình ngoại lệ có owner, reason, expiry và audit tăng cường; không phải bypass bí mật.

- #chk *Threat model data flow* #p1 #tag("DP") — Vẽ actor, asset, trust boundary và nơi dữ liệu được model/tool xử lý.
- #chk *Prompt injection defense in depth* #p0 #tag("SECURITY") — Data/instruction separation, tool allowlist, sandbox, output checks và approval.
- #chk *Indirect injection from retrieval* #p0 #tag("SECURITY") — Test tài liệu độc, web page, email, PDF và tool result.
- #chk *Sensitive information disclosure* #p0 #tag("SECURITY") — Access control ở source/retriever/tool; prompt không phải authorization.
- #chk *Improper output handling* #p1 #tag("SECURITY") — Validate/escape trước HTML, SQL, shell, markdown link, file path hoặc code execution.
- #chk *Insecure deserialization* #p1 #tag("SECURITY") — Không deserialize object/payload model-generated hoặc cache chưa tin cậy.
- #chk *SQL injection* #p0 #tag("SECURITY") — Parameterized query/allowlisted sort/filter; không nối dynamic SQL từ model/user.
- #chk *Mass assignment* #p1 #tag("SECURITY") — DTO allowlist field; không bind thẳng LLM JSON vào entity.
- #chk *SSRF* #p1 #tag("SECURITY") — HTTP tool không được tự do gọi URL từ model; kiểm tra scheme, DNS, redirect và private IP.
- #chk *Data poisoning* #p1 #tag("SECURITY") — Provenance, ingestion approval, content scan và versioned index.
- #chk *Model/SDK supply chain* #p1 #tag("SECURITY") — Pin version, checksum/image provenance, dependency scan và model source review.
- #chk *PII policy* #p1 #tag("BP") — Classify, minimize, redact, encrypt, retention, consent và deletion.
- #chk *Policy-as-code* #p1 #tag("API") — Quy tắc action/risk/tenant kiểm tra trước tool execution.
- #chk *High-impact decision boundary* #p1 #tag("SECURITY") — Legal, financial, health, employment cần human review và explanation.
- #chk *Adversarial eval* #p1 #tag("LAB") — Prompt injection, data exfiltration, SSRF, tool abuse, cross-tenant và output encoding.

=== 16.4 Guardrail failure matrix


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Failure*], [*Nguyên nhân*], [*Control*],
  [Prompt-only authorization], [Model bị injection hoặc hiểu sai], [Authorization trong service/tool boundary],
  [Stale approval], [State/target đổi sau khi user duyệt], [Bind approval vào immutable action digest; hết hạn khi diff đổi],
  [Over-broad credential], [Một tool compromise ảnh hưởng toàn tenant], [Scoped short-lived capability token],
  [Output filter bypass], [Encoding/format lạ hoặc stream từng phần], [Canonicalize, validate complete artifact, safe renderer],
  [Policy disagreement], [Các service đánh giá khác nhau], [Central policy contract, version tagging, conformance tests],
  [Silent guardrail failure], [Timeout/error được hiểu thành allow], [Explicit fail mode theo risk tier],
)
]


- #chk *LAB — permission gateway* #p1 #tag("LAB") — Xây gateway đánh giá
`principal × action × resource × context`; test deny-by-default, expired approval,
delegated worker, cross-tenant access, stale diff và policy service timeout.

== Chuyên đề — Human-in-the-loop Design


#callout[*Điểm học chính của HITL.* Human-in-the-loop không có nghĩa “hỏi user sau mỗi bước”. Human chỉ tham gia tại decision boundary nơi judgment, authority hoặc accountability của con người tạo thêm giá trị.]


=== Khi nào cần con người


- #chk *Approval* #p1 #tag("CONCEPT") — Con người cấp authority cho một action đã mô tả chính xác.
- #chk *Clarification* #p1 #tag("CONCEPT") — Thiếu intent/constraint quan trọng; không đoán thay user.
- #chk *Review* #p1 #tag("CONCEPT") — Con người kiểm tra artifact/output trước khi sử dụng.
- #chk *Escalation* #p1 #tag("CONCEPT") — Agent bị blocked, vượt risk/uncertainty/cap hoặc gặp policy conflict.
- #chk *Exception handling* #p0 #tag("CONCEPT") — Trường hợp ngoài policy thông thường cần owner chịu trách nhiệm.
- #chk *Human override* #p1 #tag("CONCEPT") — Người có quyền sửa/deny kết quả; override phải được audit.
- #chk *Selective autonomy* #p2 #tag("BP") — Tự động hóa R0–R2 khi controls đủ mạnh; tăng human control theo risk và irreversibility.
- #chk *Uncertainty alone is insufficient* #p1 #tag("PITFALL") — Confidence model không calibrated; kết hợp risk, novelty, evidence và action impact.

=== Interrupt → review → resume protocol


```mermaid
stateDiagram-v2
    [*] --> Running
    Running --> Waiting: interrupt + checkpoint
    Waiting --> Running: approve or provide input
    Waiting --> Replanning: reject or modify
    Waiting --> Expired: deadline reached
    Replanning --> Running
    Running --> Completed
    Expired --> Completed: safe stop
```


- #chk *Interrupt boundary* #p1 #tag("CONCEPT") — Pause trước side effect hoặc decision quan trọng, không pause giữa transaction mơ hồ.
- #chk *Review packet* #p1 #tag("API") — Goal, proposed action, exact target/diff, evidence, alternatives, risk, cost và expiry.
- #chk *Typed human response* #p1 #tag("API") — `approve`, `reject`, `modify`, `request_more_evidence`; tránh parse câu trả lời tự do cho action nguy hiểm.
- #chk *Checkpoint before wait* #p1 #tag("BP") — Persist state và pending action trước khi gửi yêu cầu review.
- #chk *Approval token* #p1 #tag("API") — One-time, scoped, expiring token gắn với action digest và reviewer identity.
- #chk *Resume semantics* #p1 #tag("CONCEPT") — Revalidate state, permission, freshness và idempotency trước commit.
- #chk *Timeout/default action* #p1 #tag("SECURITY") — Hết hạn phải dừng hoặc fallback an toàn; không mặc định approve.
- #chk *Concurrent modification* #p1 #tag("PITFALL") — Resource đổi trong lúc chờ; invalid approval và tạo review packet mới.

=== Thiết kế trải nghiệm review


- #chk *Decision-focused UI* #p1 #tag("BP") — Hiển thị điều gì sẽ xảy ra và phần nào cần quyết định, không đổ raw trace cho reviewer.
- #chk *Diff/preview* #p1 #tag("BP") — Cho thấy before/after, recipient, amount hoặc affected resources.
- #chk *Evidence provenance* #p1 #tag("BP") — Nguồn và freshness của claim quan trọng phải mở xem được.
- #chk *No dark patterns* #p1 #tag("SECURITY") — Approve/reject cân bằng, không dùng wording ép duyệt.
- #chk *Reviewer routing* #p1 #tag("CONCEPT") — Chọn đúng owner/domain expert và fallback khi họ unavailable.
- #chk *Separation of duties* #p1 #tag("SECURITY") — Tác vụ rủi ro cao có thể yêu cầu người duyệt khác người khởi tạo.
- #chk *Reviewer load metric* #p1 #tag("METRIC") — Queue time, approval rate, rejection reason, review duration và alert fatigue.
- #chk *Automation learning boundary* #p1 #tag("SECURITY") — Không tự biến các approval cũ thành quyền vĩnh viễn nếu chưa có explicit policy change.

=== HITL failure matrix


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Failure*], [*Dấu hiệu*], [*Control*],
  [Rubber-stamp approval], [Approval gần 100%, review cực nhanh], [Better preview, sampling audit, reduce noisy requests],
  [Approval fatigue], [Queue dài, reviewer bỏ qua], [Risk threshold, batch low-risk items, automation policy],
  [Wrong reviewer], [Người duyệt không có ownership/expertise], [Reviewer routing + role/attribute checks],
  [Contextless escalation], [Human phải điều tra lại từ đầu], [Structured escalation artifact],
  [Stale approval], [Target thay đổi khi đang chờ], [Action digest + revalidation],
  [Lost interrupt], [Deploy/crash làm mất pending review], [Durable checkpoint + resumable queue],
  [Indefinite waiting], [Run treo không có owner/deadline], [SLA, expiry, reassignment, safe stop],
)
]


- #chk *HITL eval* #p1 #tag("METRIC") — Đo precision/recall của escalation, preventable harm,
reviewer agreement, time-to-decision và task completion sau resume.
- #chk *LAB — approval workflow* #p1 #tag("LAB") — Workflow draft → interrupt → typed review →
resume; test reject/modify, approval expiry, resource changed, duplicate resume,
reviewer unavailable và process crash.

== 17. AI backend integration


- #chk *AI application service* #p1 #tag("CONCEPT") — Orchestrate prompt, retrieval, tools, policy và persistence.
- #chk *Async job* #p1 #tag("CONCEPT") — Tách request HTTP khỏi long-running generation/ingestion.
- #chk *Outbox pattern* #p1 #tag("DP") — Ghi business state và event intent cùng transaction; worker publish/execute sau.
- #chk *Queue/backpressure* #p1 #tag("CONCEPT") — Giới hạn ingest/embedding/agent jobs và dead-letter.
- #chk *PostgreSQL transaction boundary* #p0 #tag("CONCEPT") — DB transaction không bao trùm provider/tool call.
- #chk *Redis cache* #p0 #tag("LIB") — Session, rate limit, result cache hoặc ephemeral state; không mặc định là source of truth.
- #chk *Vector index migration* #p0 #tag("CONCEPT") — Build index mới, dual-read/dual-write, verify rồi cutover.
- #chk *API streaming/SSE/WebSocket* #p0 #tag("API") — Gửi token/event nhưng phải xử lý reconnect và resume.
- #chk *OpenTelemetry* #p1 #tag("LIB") — Trace context và metrics; tránh high-cardinality raw prompt.
- #chk *OpenTelemetry Python instrumentation* #p1 #tag("LIB") — Instrument FastAPI, provider SDK, retrieval, tool và database spans.
- #chk *Spring AI observability* #p0 #tag("API") — Chỉ áp dụng ở integration track khi AI call nằm trong Spring Boot.
- #chk *NFR budget* #p1 #tag("CONCEPT") — SLA/SLO, cost per task, max latency và data freshness là product contract.
- #chk *LAB — Python production vertical slice* #p1 #tag("LAB") — FastAPI + Postgres/pgvector + async ingest + tool approval + eval gate + traces.
- #chk *LAB — optional Java integration* #p1 #tag("LAB") — Spring Boot gọi Python AI service qua typed HTTP/gRPC contract, không chuyển AI runtime sang Java.

=== M2 Definition of Done


- #chk Basic agent có typed state, explicit stop reason và manual bounded loop đã được test trước khi thêm graph framework.
- #chk Có hard caps cho step/tool/time/cost, progress invariant và retry/replan/escalation policy.
- #chk Action rủi ro có preview, approval, expiry và audit; model không tự cấp quyền cho chính nó.
- #chk Có provider adapter và model/prompt/index version.
- #chk Có timeout, retry budget, rate/concurrency limit, cost/token budget và cancellation.
- #chk Có versioned eval cases, slice coverage, contamination control, adversarial/security set và regression gate theo risk.
- #chk LLM judge được pin/calibrate bằng human sample; task không deterministic có repeated trials và báo variance/uncertainty phù hợp.
- #chk Production feedback chỉ vào eval/training set qua redaction, provenance, sampling và labeling workflow; không coi click/retry là ground truth trực tiếp.
- #chk Có trace cho model/retrieval/tool; raw prompt/completion được redact hoặc opt-in.
- #chk Có authorization/tenant filter ở data và tool layer, không dựa vào prompt.
- #chk Có idempotency/dedupe cho mutation và async resume.
- #chk Có runbook, rollback bundle và một sự cố mô phỏng đã được chứng minh.

#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= MỨC 3 — ADVANCED AGENT INFRASTRUCTURE


#callout[Basic agent đã nằm ở Mức 2. Chỉ học Mức 3 khi nó tạo ra nhu cầu infrastructure thật: run dài hơn request timeout, cần durable checkpoint/pause/resume/replay, nhiều capability có policy phức tạp, interoperability hoặc delegation. “Có thể dùng graph” không phải lý do đủ để thêm graph.]


== 18. Agent architecture và failure model


- #chk *Single-turn LLM call* #p2 #tag("CONCEPT") — Một request, không có durable loop.
- #chk *Workflow* #p2 #tag("CONCEPT") — Các bước xác định, deterministic branching.
- #chk *Single agent loop* #p2 #tag("CONCEPT") — Model chọn hành động trong bounded loop.
- #chk *Graph orchestration* #p2 #tag("CONCEPT") — State/node/edge rõ ràng, conditional path và checkpoint.
- #chk *Supervisor/worker* #p2 #tag("DP") — Supervisor route/delegate; worker thực hiện capability hẹp.
- #chk *Peer multi-agent* #p2 #tag("DP") — Agent trao đổi ngang; coordination và deadlock phức tạp hơn.
- #chk *Blackboard/shared state* #p2 #tag("DP") — Agent đọc/ghi workspace chung; cần ownership/version.
- #chk *Failure domain* #p2 #tag("CONCEPT") — Model, provider, tool, DB, queue, checkpoint, human và network có failure riêng.
- #chk *Blast radius* #p2 #tag("CONCEPT") — Một lỗi có thể ảnh hưởng run, tenant, queue hoặc side effect ngoài hệ thống.
- #chk *Compensation* #p2 #tag("DP") — Saga-like undo/repair cho side effect không atomic.
- #chk *Escalation policy* #p1 #tag("CONCEPT") — Chuyển người khi risk, uncertainty, cap hoặc policy threshold vượt.
- #chk *Autonomy budget* #p2 #tag("CONFIG") — Giới hạn quyền, tiền, thời gian, tool calls và dữ liệu agent được phép dùng.

== 19. Graph Engineering


=== 19.1 Graph primitives


- #chk *State schema* #p2 #tag("API") — Typed state, reducer/merge rule, version và ownership.
- #chk *Node* #p2 #tag("CONCEPT") — Đơn vị compute; ghi rõ input/output, side effect và retry semantics.
- #chk *Edge* #p2 #tag("CONCEPT") — Chuyển state; deterministic hoặc LLM-routed.
- #chk *Conditional edge* #p2 #tag("CONCEPT") — Branch dựa trên state/guard, không chỉ text tự do.
- #chk *Cycle* #p2 #tag("CONCEPT") — Lặp có invariant, cap và termination condition.
- #chk *Reducer* #p2 #tag("CONCEPT") — Merge parallel updates; phải xác định conflict semantics.
- #chk *Subgraph* #p2 #tag("CONCEPT") — Đóng gói workflow con, contract state rõ.
- #chk *Checkpointer* #p2 #tag("CONCEPT") — Lưu execution state để resume/replay.
- #chk *Interrupt / human-in-the-loop* #p2 #tag("CONCEPT") — Pause tại boundary, lưu state, nhận input rồi resume.
- #chk *Durable execution* #p2 #tag("CONCEPT") — Process crash/deploy không làm mất progress.
- #chk *Replay / time travel* #p1 #tag("CONCEPT") — Reproduce state transitions bằng event/checkpoint; side effect phải không chạy lại bừa bãi.
- #chk *Graph versioning* #p2 #tag("BP") — Run đang chạy giữ graph version; deploy mới không đổi semantics giữa chừng.
- #chk *State migration* #p2 #tag("BP") — Migration có version, test và fallback cho checkpoint cũ.

=== 19.2 Failure model của graph


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Failure*], [*Hậu quả*], [*Control/recovery*],
  [Node process crash], [Mất in-memory state hoặc chạy lại node], [Checkpoint trước boundary; node idempotent],
  [Duplicate resume], [Side effect chạy hai lần], [Idempotency key, dedupe record, status query],
  [Stale checkpoint], [Resume bằng schema/graph cũ], [Version pin, migration hoặc fail closed],
  [Conditional edge sai], [Đi nhánh nguy hiểm/không đạt goal], [Guard typed, eval branch, policy check],
  [Loop non-termination], [Cost/latency vô hạn], [Max steps, deadline, progress invariant, oscillation detector],
  [Partial side effect], [DB/remote chỉ commit một phần], [Outbox/compensation/reconciliation],
  [Human timeout], [Workflow bị treo], [Escalation, expiry, cancel/resume policy],
)
]


- #chk *LAB — durable graph* #p2 #tag("LAB") — Workflow research → draft → approval; kill process ở từng node, resume và chứng minh không gửi email hai lần.

== 20. Advanced Loop Reliability


- #chk *Loop contract* #p2 #tag("CONCEPT") — Input, state invariant, action set, observation, stop condition và failure output.
- #chk *Progress measure* #p2 #tag("CONCEPT") — Số evidence mới, task checklist, state distance hoặc score phải tiến triển.
- #chk *Termination proof sketch* #p2 #tag("BP") — Nêu lý do loop giảm budget hoặc tiến đến absorbing state.
- #chk *Step budget* #p2 #tag("CONFIG") — Hard max iteration.
- #chk *Tool budget* #p2 #tag("CONFIG") — Limit theo tool, risk và tenant.
- #chk *Time budget* #p2 #tag("CONFIG") — Deadline end-to-end và per-step timeout.
- #chk *Cost budget* #p2 #tag("CONFIG") — Token/provider/tool cost.
- #chk *Retry budget* #p2 #tag("CONFIG") — Retry không được vô hạn hoặc lồng multiplicatively.
- #chk *Backoff/jitter* #p2 #tag("DP") — Retry transient failure.
- #chk *Replan trigger* #p2 #tag("CONCEPT") — Evidence contradicts plan, tool unavailable, user changes goal.
- #chk *Reflection* #p1 #tag("CONCEPT") — Critic/evaluator sửa plan/output; có thể gây self-confirmation bias.
- #chk *Verifier* #p2 #tag("CONCEPT") — Rule/typed test/external source/human kiểm tra output.
- #chk *Oscillation detection* #p2 #tag("CONCEPT") — Cùng query/tool/action lặp lại không tạo state change.
- #chk *Stagnation timeout* #p2 #tag("CONFIG") — Dừng nếu N bước không có progress.
- #chk *Escalation artifact* #p2 #tag("CONCEPT") — Đưa người lý do, evidence, attempted actions và next options.
- #chk *LAB — bounded loop* #p2 #tag("LAB") — Tạo loop tự sửa JSON tối đa 3 lần; sau đó human review, không retry lỗi schema vô hạn.

=== Loop failure matrix


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Mẫu lỗi*], [*Dấu hiệu trace*], [*Chiến lược*],
  [Retry storm], [Nhiều call giống nhau, 429 tăng], [Global retry budget + breaker + jitter],
  [Self-confirmation], [Critic luôn “pass” output của chính model], [Verifier độc lập hoặc golden checks],
  [Oscillation], [A→B→A, cùng tool args], [State hash/history detector, replan],
  [Premature stop], [Chưa đủ evidence nhưng `done=true`], [Required checklist + answerability gate],
  [Endless research], [Retrieval liên tục, không synthesis], [Evidence quota, marginal-gain threshold],
  [Cost runaway], [Token/steps tăng không bounded], [Hard budget kill + partial result],
)
]


== 21. Harness Engineering


#callout[Harness là runtime “đỡ” agent: chuẩn bị context, expose capability, kiểm tra hành động, quản lý state, sandbox, artifact, approval và recovery. Model chỉ là một component trong harness, không phải nơi đặt toàn bộ security/business invariant.]


=== 21.1 Harness components


- #chk *Context design* #p2 #tag("CONCEPT") — Chọn instruction, memory, evidence, tool result và output contract.
- #chk *Context expansion* #p2 #tag("CONCEPT") — Retrieval, file loading, web/browser, code execution hoặc delegated task.
- #chk *Context compaction* #p2 #tag("CONCEPT") — Summary/extract/evict theo policy.
- #chk *Tool registry* #p2 #tag("CONCEPT") — Capability catalog, schema, auth, availability và version.
- #chk *Tool policy* #p1 #tag("SECURITY") — Allow/deny theo actor, tenant, risk, environment và approval.
- #chk *Tool verification* #p2 #tag("CONCEPT") — Validate args/result, freshness, ownership và invariants.
- #chk *Correction loop* #p2 #tag("CONCEPT") — Sửa args/query/plan dựa trên lỗi typed, không tự nuốt exception.
- #chk *System-level verification* #p2 #tag("CONCEPT") — Kiểm tra task outcome, side effect, citation và policy sau toàn run.
- #chk *Sandbox* #p2 #tag("SECURITY") — CPU/memory/time/network/filesystem capability limits.
- #chk *Artifact store* #p2 #tag("CONCEPT") — Lưu file/report/diff có checksum, owner, TTL và provenance.
- #chk *Initialization phase* #p2 #tag("CONCEPT") — Chuẩn bị environment, task state và credentials scoped.
- #chk *Execution phase* #p2 #tag("CONCEPT") — Agent làm việc trong environment đã chuẩn bị.
- #chk *Human escalation* #p2 #tag("CONCEPT") — Chuyển người khi cap/risk/uncertainty vượt.
- #chk *Run ledger* #p2 #tag("CONCEPT") — Audit event, budget, approval, state transition và result.

=== 21.2 Harness failure model


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Failure*], [*Rủi ro*], [*Control*],
  [Tool quyền quá rộng], [Exfiltration hoặc side effect ngoài ý muốn], [Capability allowlist, scoped credential, sandbox],
  [Context phình to], [Cost/latency, policy bị truncation], [Budget, compaction, priority ordering],
  [Verification chỉ bằng model], [Sai lầm tự xác nhận], [Rule/schema/external check/human],
  [Init và execute trộn lẫn], [Resume không reproducible], [Tách phase, version environment],
  [Artifact không có provenance], [Không biết output từ đâu], [Checksum, source/version, trace ID],
  [Human approval mơ hồ], [Approve nhầm action], [Preview/diff, risk summary, expiry],
  [Secret trong context], [Leak qua output/tool], [Secret broker, redaction, scoped token],
)
]


- #chk *LAB — harness sandbox* #p2 #tag("LAB") — Agent sửa file trong workspace giả lập; chỉ được đọc 2 thư mục, ghi artifact mới, không network, có diff/approval.

== 22. MCP và interoperability


- #chk *Model Context Protocol (MCP)* #p2 #tag("STANDARD") — Giao thức client–server cho model-facing tools/context.
- #chk *MCP host/client/server* #p2 #tag("CONCEPT") — Host điều phối; client kết nối server; server expose capability.
- #chk *Tools* #p2 #tag("CONCEPT") — Function có schema để model/client gọi.
- #chk *Resources* #p2 #tag("CONCEPT") — Dữ liệu/context được đọc theo URI/provenance.
- #chk *Prompts* #p2 #tag("CONCEPT") — Template/prompt surface do server cung cấp.
- #chk *JSON-RPC messages* #p2 #tag("UTH") — Request/response/notification và error semantics.
- #chk *Capability negotiation* #p2 #tag("CONCEPT") — Client/server công bố feature hỗ trợ.
- #chk *Tool discovery* #p2 #tag("CONCEPT") — Liệt kê/chọn tool; không đồng nghĩa được authorize.
- #chk *Authentication/authorization* #p2 #tag("SECURITY") — Identity, scope, tenant, consent và audit.
- #chk *MCP server trust* #p2 #tag("SECURITY") — Server/resource là untrusted dependency cho đến khi review.
- #chk *Prompt injection through resource* #p1 #tag("SECURITY") — Resource có thể chứa instruction độc.
- #chk *Provenance* #p2 #tag("CONCEPT") — Nguồn, version, server identity và retrieval timestamp.
- #chk *Tasks/durable operations* #p2 #tag("CONCEPT") — Theo dõi operation dài; kiểm tra version spec trước production.
- #chk *LAB — local MCP server* #p2 #tag("LAB") — Expose read-only knowledge tool, schema strict, auth stub, provenance và injection test.

#callout[*Version discipline:* MCP là spec đang phát triển. Pin spec/SDK version trong project, chỉ dùng feature đã được tài liệu version đó cam kết; không xây production dựa vào draft hoặc release candidate chưa có compatibility plan.]


== 23. Multi-agent và delegation


- #chk *Agent role boundary* #p2 #tag("CONCEPT") — Mỗi agent có goal, tools, data và output contract hẹp.
- #chk *Supervisor pattern* #p2 #tag("DP") — Route/delegate/merge; supervisor không nên tự làm mọi việc.
- #chk *Worker pattern* #p2 #tag("DP") — Worker chuyên retrieval, coding, verification hoặc domain task.
- #chk *Handoff* #p2 #tag("API") — Chuyển conversation/state với acceptance criteria và authority rõ.
- #chk *Blackboard* #p2 #tag("DP") — Shared workspace; cần locking/version/ownership.
- #chk *Message contract* #p2 #tag("API") — Typed task, evidence, status, error và confidence.
- #chk *Shared-memory contamination* #p2 #tag("SECURITY") — Worker ghi fact/plan không đúng làm agent khác sai.
- #chk *Coordination overhead* #p2 #tag("TRADEOFF") — N agent tăng call/cost/latency và failure surface.
- #chk *Deadlock/livelock* #p2 #tag("CONCEPT") — Agent chờ nhau hoặc lặp delegate.
- #chk *Error propagation* #p2 #tag("CONCEPT") — Worker error phải typed và supervisor phải quyết định retry/escalate.
- #chk *Privacy boundary* #p2 #tag("SECURITY") — Chia đúng dữ liệu cần thiết, không broadcast toàn tenant context.
- #chk *LAB — supervisor with two workers* #p2 #tag("LAB") — Supervisor phân công retrieval và verifier; budget, shared state và timeout rõ.

== 24. Model adaptation và serving — tùy nhu cầu


- #chk *Prompting vs RAG vs tool vs fine-tuning* #p2 #tag("TRADEOFF") — Chọn giải pháp rẻ nhất đạt quality; không fine-tune để chữa thiếu dữ liệu runtime.
- #chk *Supervised fine-tuning (SFT)* #p2 #tag("CONCEPT") — Học format/behavior từ examples.
- #chk *Preference optimization / DPO* #p2 #tag("CONCEPT") — Tối ưu theo preference pairs; cần dataset và eval đáng tin.
- #chk *LoRA/PEFT* #p2 #tag("CONCEPT") — Fine-tune adapter ít tham số hơn full model.
- #chk *Data curation* #p2 #tag("CONCEPT") — Quality, dedup, license, PII, split và provenance quan trọng hơn số lượng.
- #chk *Adaptation split discipline* #p2 #tag("BP") — Mở lại ML/DL literacy on-demand; với SFT/DPO phải tách train/dev/holdout theo source/task/time và loại near-duplicate để tránh leakage.
- #chk *Model card* #p2 #tag("CONCEPT") — Capability, limitation, dataset, safety và intended use.
- #chk *Quantization* #p2 #tag("TRADEOFF") — Giảm memory/cost, có thể giảm quality/compatibility.
- #chk *Batching* #p2 #tag("CONCEPT") — Dynamic/static batching tăng throughput.
- #chk *KV cache* #p2 #tag("UTH") — Tái sử dụng attention state trong generation; ảnh hưởng memory/latency.
- #chk *Continuous batching* #p2 #tag("CONCEPT") — Scheduler ghép request đang chạy để tăng GPU utilization.
- #chk *vLLM/TGI/serving runtime* #p2 #tag("LIB") — Chỉ học khi cần self-hosted inference.
- #chk *Speculative decoding* #p2 #tag("CONCEPT") — Draft model + verify model; benchmark workload cụ thể.
- #chk *GPU scheduling* #p2 #tag("CONCEPT") — Capacity, memory, autoscaling, warm pool và noisy neighbor.
- #chk *LAB — adaptation decision* #p2 #tag("LAB") — Cùng dataset, so sánh prompt+RAG, tool và LoRA theo quality/latency/cost; ghi decision record.

== 25. Long-running agent và autonomy


- #chk *Long-running task* #p2 #tag("CONCEPT") — Task vượt process/request lifetime, cần durable state và resume.
- #chk *Initialization/execution separation* #p2 #tag("DP") — Init môi trường một lần; execution có thể restart.
- #chk *Checkpoint cadence* #p1 #tag("CONFIG") — Checkpoint trước/sau side effect hoặc phase quan trọng.
- #chk *Lease/heartbeat* #p2 #tag("CONCEPT") — Xác định worker còn sống; reclaim job quá hạn.
- #chk *Human escalation on cap* #p2 #tag("BP") — Không âm thầm tiếp tục khi vượt time/cost/permission.
- #chk *Autonomy level* #p2 #tag("CONCEPT") — Suggest → draft → execute with approval → bounded auto-execute.
- #chk *High-risk action gate* #p2 #tag("SECURITY") — Delete, payment, legal submission, external communication cần explicit approval.
- #chk *Trust calibration* #p2 #tag("CONCEPT") — Quyền tự chủ tương ứng confidence, evidence và reversibility.
- #chk *Rollback/compensation* #p2 #tag("DP") — Undo hoặc repair side effect đã thực thi.
- #chk *Run expiration* #p2 #tag("CONFIG") — TTL cho credentials, links, temporary artifacts và pending approvals.
- #chk *LAB — overnight worker* #p2 #tag("LAB") — Ingest 1.000 documents bằng queue/checkpoint/resume/dead-letter và human review sample.

== 26. Advanced Definition of Done


Một Mức 3 feature chưa “xong” nếu chỉ có graph hoặc prompt chạy được. Phải trả lời
được các câu hỏi sau:

- #chk *Trigger:* #p2 Vì sao workflow/agent/graph cần tồn tại thay vì function call?
- #chk *State:* #p2 State schema, owner, version, persistence và migration là gì?
- #chk *Guarantee:* #p2 Invariant nào được code/schema đảm bảo?
- #chk *Failure model:* #p2 Model/provider/tool/DB/queue/checkpoint/human/network fail thế nào?
- #chk *Recovery:* #p2 Retry, replan, compensate, reconcile, resume hay escalate?
- #chk *Termination:* #p2 Điều kiện dừng, hard caps và progress invariant là gì?
- #chk *Security:* #p2 Trust boundary, least privilege, injection, SSRF, data isolation và approval?
- #chk *Observability:* #p1 Trace/span, prompt/model/tool/graph version, budget và failure taxonomy?
- #chk *Evaluation:* #p2 Golden/adversarial/online eval nào chứng minh quality?
- #chk *Cost/NFR:* #p2 p95 latency, token/cost per task, availability, freshness và SLO?
- #chk *Rollback:* #p2 Rollback model/prompt/index/tool schema/graph version thế nào?
- #chk *Lab evidence:* #p2 Có test hoặc trace tái hiện ít nhất một happy path và ba failure path?

#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= 27. Pattern failure matrix — tra cứu nhanh


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Pattern*], [*Dùng để*], [*Failure phổ biến*], [*Control bắt buộc*],
  [Prompt template], [Ổn định instruction/output], [Injection, prompt drift, schema mismatch], [Version, delimiters, schema, regression eval],
  [RAG], [Ground answer bằng data runtime], [Chunk/ACL/stale index/citation sai], [Retrieval eval, provenance, ACL filter, abstention],
  [Tool calling], [Kết nối capability/action], [Args sai, SSRF, partial side effect], [Typed schema, auth, timeout, idempotency, approval],
  [Agent loop], [Xử lý task nhiều bước], [Infinite loop, retry storm, cost runaway], [Step/time/cost caps, progress, verifier],
  [Graph], [Durable branching/resume], [Duplicate resume, stale checkpoint], [Versioned state, checkpoint, idempotent nodes],
  [Harness], [Runtime policy và verification], [Tool quá quyền, context overflow], [Sandbox, capability policy, compaction, audit],
  [MCP], [Interoperability], [Server/resource injection, auth gap], [Pin spec, trust review, scope, provenance],
  [Multi-agent], [Chia task/chuyên môn], [Deadlock, shared-state corruption], [Message contract, ownership, timeout, supervisor],
  [Fine-tuning], [Behavior/format ổn định], [Data leakage, overfit, model drift], [Dataset provenance, holdout eval, rollback],
  [Model routing], [Quality/cost/latency trade-off], [Fallback schema/quality khác], [Capability matrix, contract tests, canary],
)
]


#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= 28. Các flow nên vẽ và debug


== 28.1 LLM application request flow


```mermaid
flowchart TD
    U[API request] --> P[Policy + auth]
    P --> C[Context builder]
    C --> M[Model call]
    M --> T{Tool call?}
    T -- Yes --> X[Authorize + execute + verify]
    X --> M
    T -- No --> O[Validate output]
    O --> R[Response + trace + metrics]
```


== 28.2 Context composition


```mermaid
flowchart LR
    POL[Policy] --> CTX[Context]
    TASK[Task + user input] --> CTX
    MEM[Memory] --> CTX
    EVID[Retrieved evidence] --> CTX
    TOOL[Verified tool results] --> CTX
    CTX --> LLM[LLM]
```


== 28.3 Evaluation loop


```mermaid
flowchart LR
    CHANGE[Prompt/model/RAG/tool change] --> RUN[Offline eval]
    RUN --> JUDGE[Rule + judge + human sample]
    JUDGE --> GATE{Pass?}
    GATE -- No --> FIX[Diagnose failure taxonomy]
    FIX --> CHANGE
    GATE -- Yes --> CANARY[Canary / shadow]
    CANARY --> PROD[Production traces]
```


== 28.4 Graph execution với approval


```mermaid
flowchart TD
    S[State] --> N1[Retrieve / plan]
    N1 --> N2[Draft action]
    N2 --> H{Human approval}
    H -- Reject --> END1[Compensate / stop]
    H -- Approve --> N3[Execute idempotently]
    N3 --> V[Verify + checkpoint]
    V --> END2[Final artifact]
```


#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= 29. Quick reference — class, API và tool


#align(center)[
#table(
  columns: (1fr, 1fr, 1fr, 1fr),
  align: (x, y) => if y == 0 { center + horizon } else { left + horizon },
  fill: (x, y) => if y == 0 { rgb("#f1f5f9") } else if calc.even(y) { rgb("#f8fafc") } else { none },
  stroke: 0.4pt + rgb("#cbd5e1"),
  [*Nhu cầu*], [*Python-first track*], [*Java/Spring integration track*], [*Bằng chứng cần kiểm tra*],
  [Project/runtime], [`uv`, `pyproject.toml`, virtualenv, FastAPI/Uvicorn], [Spring Boot service chỉ khi cần integration], [Reproducible environment],
  [Tabular ML], [scikit-learn `Pipeline`, `ColumnTransformer`, estimator/metric], [Consume artifact qua typed Python service hoặc portable runtime], [Split/leakage, baseline, CV, threshold, inference parity],
  [Deep Learning], [PyTorch `Dataset`/`DataLoader`, `nn.Module`, optimizer, checkpoint], [Java chỉ consume inference contract khi có lý do], [Shape/dtype/device, tiny-set overfit, best/last state],
  [Computer Vision], [torchvision transforms/models + task metric], [Typed image/result API hoặc supported exported runtime], [Geometry/label map, overlay, per-class/size metric],
  [NLP model], [Transformers/Datasets/Evaluate hoặc TF-IDF baseline], [Typed text/result API], [Tokenizer pairing, truncation, label alignment, language slices],
  [Chat], [Official provider SDK + `httpx.AsyncClient`], [Spring AI `ChatClient`/`ChatModel`], [Usage, timeout, finish reason],
  [Structured output], [Pydantic models/JSON Schema], [DTO/record + converter/validator], [Schema pass/fail, malformed output],
  [Embedding], [Embeddings API + tokenizer], [Spring AI `EmbeddingModel`], [Dimension, cosine/recall],
  [Vector search], [pgvector client/SQLAlchemy/psycopg], [Spring AI `VectorStore`], [Recall\@k, filter correctness],
  [RAG], [Custom pipeline; optional LangChain components], [Spring AI advisors khi phù hợp], [Groundedness, citation],
  [Tools], [JSON Schema/function tools + Python validators], [`@Tool`, `ToolCallback`], [Auth, idempotency, audit],
  [Memory], [Typed state/store, Redis/Postgres], [`ChatMemory`, memory advisors], [TTL, delete, poisoning],
  [Agent loop], [Custom bounded loop; optional LangGraph], [Java state machine khi cần], [Caps, stop reason, trace],
  [Graph], [LangGraph StateGraph/checkpointer khi durable graph có trigger], [Custom orchestration service], [Resume/replay/version],
  [MCP], [MCP Python SDK/client/server], [MCP client/server SDK nếu Java host], [Capability/auth/provenance],
  [Observability], [OpenTelemetry Python + provider instrumentation], [Spring AI observations + OTel], [Trace IDs, redaction],
  [Evaluation], [pytest, dataset runner, custom eval harness], [JUnit/testcontainers consumer tests], [Frozen set, regression delta],
  [Storage], [PostgreSQL, pgvector, Redis, object store], [Java repository/transaction boundary], [Durability, ACL, retention],
  [Async], [`asyncio`, worker/queue SDK, outbox consumer], [Spring events/queue/Kafka producer], [Backpressure, DLQ, dedupe],
)
]


#callout[API names có thể đổi theo release. Dùng bảng này để định vị concept, sau đó đọc reference đúng version thay vì copy code từ một version khác.]


#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= 30. Anti-patterns cần tránh


- #chk *AI-generated code = understanding* #p2 — Notebook chạy được không chứng minh data/split/loss/metric đúng; phải qua quality gates và tự trace một sample.
- #chk *Random split everywhere* #p2 — Split frame/sentence/row ngẫu nhiên dù cùng source/user/time làm leakage.
- #chk *Preprocess before split* #p2 — Fit scaler/imputer/vocabulary/feature selection trên toàn data rồi mới cross-validation.
- #chk *Architecture before baseline* #p2 — Xây YOLO/Transformer phức tạp khi loader, metric và baseline đơn giản chưa được chứng minh.
- #chk *Test-set tuning* #p2 — Chọn hyperparameter/checkpoint/threshold dựa trên test và vẫn gọi đó là unbiased result.
- #chk *Metric shopping* #p2 — Báo metric đẹp nhất sau khi xem kết quả thay vì metric/acceptance rule đã định trước.
- #chk *Train–eval mismatch* #p2 — Validation vẫn augmentation random hoặc quên `model.eval()`/inference preprocessing.
- #chk *Loss–target mismatch* #p2 — Softmax/sigmoid, target dtype/shape và loss không cùng semantics nhưng code vẫn có thể chạy.
- #chk *Checkpoint without contract* #p2 — Chỉ lưu `.pth` mà thiếu architecture/config/preprocessing/label map/threshold/runtime version.
- #chk *Aggregate metric only* #p2 — Không xem per-class/source/language/size/length slices và prediction thật.
- #chk *Prompt stuffing* #p2 — Nhét toàn bộ transcript/document vào context thay vì retrieval/compaction.
- #chk *RAG before problem definition* #p2 — Dùng vector DB khi task chỉ cần SQL/filter hoặc deterministic lookup.
- #chk *Vector DB without ACL* #p2 — Retrieve xong mới lọc quyền; đã quá muộn nếu data vào prompt.
- #chk *LLM judge as truth* #p2 — Dùng một model chấm mọi thứ mà không có rubric/calibration/human sample.
- #chk *God agent* #p2 — Một agent có toàn bộ tools, secrets, data và quyền production.
- #chk *Retry everything* #p2 — Retry validation/policy/permanent error hoặc retry lồng nhiều layer.
- #chk *No budget* #p2 — Không cap token, steps, time, tool calls hoặc money.
- #chk *Vague tool descriptions* #p2 — Tool name/mô tả không nói side effect, precondition, failure.
- #chk *Irreversible direct action* #p2 — Model gọi send/delete/pay mà không preview/approval/idempotency.
- #chk *Memory = full transcript* #p2 — Context tăng vô hạn, giữ cả dữ liệu không cần và prompt injection.
- #chk *Exactly-once claim* #p2 — Gọi “exactly once” thay vì chứng minh dedupe/idempotency.
- #chk *No version tags* #p2 — Không biết trace chạy prompt/model/index/tool/graph nào.
- #chk *Model swap without eval* #p2 — Thay model vì benchmark hoặc giá mà không chạy task eval.
- #chk *Multi-agent too early* #p2 — Thêm agent để chữa prompt/flow chưa rõ.
- #chk *MCP server blindly trusted* #p2 — Coi capability discovery là authorization.
- #chk *Secret in context* #p2 — Đưa API key/credential vào prompt hoặc raw trace.
- #chk *High-cardinality raw traces* #p2 — Gắn full prompt/completion/IDs nhạy cảm vào metric labels.
- #chk *Local-only tests* #p2 — Không mô phỏng timeout, duplicate resume, stale index, malicious document hoặc provider outage.

#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= 31. Milestones theo project


== Milestone 0 — AI/ML Minimum Foundation (1–2 buổi, khoảng 2–3 giờ)


- #chk Phân biệt ML, Deep Learning và Generative AI bằng một ví dụ cụ thể.
- #chk Vẽ `dataset → training → pretrained model → inference` và nói weights thay đổi ở đâu.
- #chk Giải thích vai trò của train/validation/test, overfitting và data leakage.
- #chk Giải thích neural network, loss và gradient descent ở mức trực giác, không dùng công thức.
- #chk Phân biệt pretrained model, gọi inference API, RAG và fine-tuning ở mức quyết định.

*Exit evidence:* một sơ đồ nửa trang và phần tự giải thích 5–10 phút cho các mục
trên. Sau đó chuyển sang Milestone A nếu ưu tiên LLM/RAG/Agent, hoặc Milestone M nếu
project hiện tại cần ML/DL/CV/NLP. Không cần hoàn thành cả hai nhánh tuần tự.

== Milestone M — AI-assisted Model Engineering (nhánh song song)


- #chk Hoàn thành H0 (tabular pipeline) để luyện split, leakage, baseline, metric và artifact contract.
- #chk Hoàn thành H1 (PyTorch pipeline) để đọc tensor/data/training flow và debug bằng tiny-set test.
- #chk Chọn đúng một H2 (CV) hoặc H3 (NLP) theo project; không làm cả hai chỉ để tick keyword.
- #chk Dùng context packet và sáu quality gates khi giao code cho AI.
- #chk Tự viết review note: assumptions, shape/schema, loss/metric, error slices, limitation và evidence.

*Exit evidence:* một model project qua H4 Definition of Done; bạn có thể thay dataset
hoặc pretrained model, phát hiện ít nhất ba lỗi pipeline phổ biến và giải thích vì sao
artifact đủ hoặc chưa đủ để tích hợp vào ứng dụng.

== Milestone A — Grounded assistant (Mức 1)


- #chk Python project bằng `uv`, `pyproject.toml`, type checking và pytest.
- #chk Giải thích mental model `tokens → attention/context → next-token prediction` mà không cần công thức Q/K/V.
- #chk Gọi official provider SDK trực tiếp trước khi cân nhắc LangChain hoặc orchestration framework.
- #chk FastAPI API nhận câu hỏi, auth/tenant context và streaming response.
- #chk Ingest 20–50 tài liệu có version, metadata và ACL.
- #chk Parse → chunk → embed → pgvector → retrieve → cite.
- #chk Pydantic structured answer và abstention khi thiếu evidence.
- #chk Tool read-only (search/calculator) có schema, timeout và audit.
- #chk Golden set + injection/ACL tests.
- #chk Trace ID, token/cost và latency p95.

*Exit evidence:* 80–100 case regression, report Recall\@k/citation/groundedness,
3 failure traces được giải thích, lockfile reproducible và rollback prompt/index.

== Milestone B — Basic agent + Production AI (Mức 2)


- #chk Basic agent theo loop `observe → decide → act → verify → stop`, có typed state và stop reason.
- #chk Hard caps cho step, tool call, deadline và cost; retry/replan/escalation tách rõ.
- #chk Human approval trước mutation rủi ro; manual bounded loop trước, LangGraph chỉ là optional.
- #chk Provider adapter, model routing, fallback, circuit breaker và budgets.
- #chk Context builder có priority, compaction, memory read/write policy.
- #chk Hybrid retrieval + rerank + freshness/update/delete pipeline.
- #chk Mutation tool dạng prepare → approve → commit, idempotency và compensation.
- #chk Async ingest/agent job với outbox, queue, backpressure và DLQ.
- #chk Offline eval gate, canary/shadow, redacted traces và runbook.
- #chk Threat model cho prompt injection, SQLi, SSRF, mass assignment, PII.
- #chk Optional: Spring Boot consumer gọi Python AI service qua typed HTTP/gRPC contract.

*Exit evidence:* basic agent trace thể hiện state/progress/stop reason; chaos table
cho provider/tool/queue/checkpoint; SLO/cost budget, adversarial eval và
post-incident style report.

== Milestone C — Chọn một nhánh Mức 3


Chọn *một* nhánh theo nhu cầu, không làm tất cả cùng lúc:

- #chk *Durable graph:* #p2 checkpoint, interrupt, resume, replay, graph versioning.
- #chk *Advanced loop/harness:* #p2 sandbox, artifact, independent verification, durable state và bounded autonomy.
- #chk *MCP:* #p2 server/tool/resource contract, auth, provenance và injection tests.
- #chk *Multi-agent:* #p2 supervisor/worker message contract, shared state và deadlock handling.
- #chk *Long-running worker:* #p2 queue, lease, checkpoint, escalation và overnight recovery.
- #chk *Model serving/adaptation:* #p2 LoRA/quantization/serving benchmark với rollback.

*Exit evidence:* depth contract dưới đây + happy path + ít nhất ba failure paths
được tái hiện bằng test/trace.

#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


= 32. Template ghi chú nâng cao


```text
## Keyword: <tên>

### Problem
- Tại sao function/API/LLM call bình thường chưa đủ?

### Flow position
- Input → context → model/tool/node → state → output.

### Contract
- Input schema:
- Output schema:
- Invariants:
- Authorization:

### Failure model
- Model/provider:
- Retrieval:
- Tool/network:
- State/checkpoint:
- Human/approval:

### Recovery
- Retry:
- Replan:
- Compensation:
- Reconcile:
- Escalate:
- Stop condition:

### Trade-offs
- Quality:
- Latency:
- Cost:
- Complexity:
- Privacy/security:

### Observability
- Trace/span:
- Prompt/model/tool/index/graph version:
- Metrics:
- Redaction:

### Evidence
- Unit/integration/eval/chaos/security tests:
- Happy path trace:
- Failure path traces:

### Rollback
- Feature flag:
- Model/prompt/index/tool/graph version:
```


= 33. Nguồn chính thức và tài liệu nên theo dõi


#callout[Luôn đọc tài liệu theo version đã pin trong project. Các URL dưới đây là điểm bắt đầu, không phải lời hứa rằng mọi class/property sẽ giữ nguyên tên.]


== OpenAI


- API guides: #link("https://developers.openai.com/api/docs/")
- Agents SDK: #link("https://developers.openai.com/api/docs/guides/agents")
- Function/tool calling: #link("https://developers.openai.com/api/docs/guides/function-calling")
- Structured outputs: #link("https://developers.openai.com/api/docs/guides/structured-outputs")
- Evaluation best practices: #link("https://developers.openai.com/api/docs/guides/evaluation-best-practices")
- Agent workflow evals: #link("https://developers.openai.com/api/docs/guides/agent-evals")
- Safety in Agent Builder: #link("https://developers.openai.com/api/docs/guides/agent-builder-safety")

== AI-assisted Machine Learning


- Google Machine Learning Crash Course: #link("https://developers.google.com/machine-learning/crash-course")
- scikit-learn User Guide: #link("https://scikit-learn.org/stable/user_guide.html")
- scikit-learn common pitfalls và data leakage: #link("https://scikit-learn.org/stable/common_pitfalls.html")
- scikit-learn Pipeline và composite estimators: #link("https://scikit-learn.org/stable/modules/compose.html")
- scikit-learn model evaluation: #link("https://scikit-learn.org/stable/modules/model_evaluation.html")
- scikit-learn cross-validation: #link("https://scikit-learn.org/stable/modules/cross_validation.html")

== Deep Learning và PyTorch


- PyTorch Learn the Basics: #link("https://docs.pytorch.org/tutorials/beginner/basics/intro.html")
- PyTorch `Dataset` và `DataLoader`: #link("https://docs.pytorch.org/docs/stable/data.html")
- PyTorch autograd: #link("https://docs.pytorch.org/tutorials/beginner/basics/autogradqs_tutorial.html")
- PyTorch optimization loop: #link("https://docs.pytorch.org/tutorials/beginner/basics/optimization_tutorial.html")
- PyTorch reproducibility: #link("https://docs.pytorch.org/docs/stable/notes/randomness.html")
- PyTorch serialization semantics: #link("https://docs.pytorch.org/docs/stable/notes/serialization.html")

== Computer Vision


- Torchvision transforms v2: #link("https://docs.pytorch.org/vision/stable/transforms.html")
- Torchvision object detection fine-tuning tutorial: #link("https://docs.pytorch.org/tutorials/intermediate/torchvision_tutorial.html")
- Torchvision model documentation: #link("https://docs.pytorch.org/vision/stable/models.html")

== NLP


- Transformers task guides: #link("https://huggingface.co/docs/transformers/tasks/sequence_classification")
- Transformers token classification/NER: #link("https://huggingface.co/docs/transformers/tasks/token_classification")
- Datasets preprocessing và tokenization: #link("https://huggingface.co/docs/datasets/use_dataset")
- Evaluate: #link("https://huggingface.co/docs/evaluate/index")

== Python runtime và application stack


- Python documentation: #link("https://docs.python.org/3/")
- `pyproject.toml` specification: #link("https://packaging.python.org/en/latest/specifications/pyproject-toml/")
- uv documentation: #link("https://docs.astral.sh/uv/")
- FastAPI documentation: #link("https://fastapi.tiangolo.com/")
- Pydantic documentation: #link("https://docs.pydantic.dev/latest/")
- HTTPX documentation: #link("https://www.python-httpx.org/")
- pytest documentation: #link("https://docs.pytest.org/")
- Ruff documentation: #link("https://docs.astral.sh/ruff/")
- SQLAlchemy documentation: #link("https://docs.sqlalchemy.org/")
- OpenTelemetry Python: #link("https://opentelemetry.io/docs/languages/python/")

== Anthropic và agent safety


- Measuring agent autonomy: #link("https://www.anthropic.com/research/measuring-agent-autonomy")
- Trustworthy agents: #link("https://www.anthropic.com/research/trustworthy-agents")
- Long-running Claude pattern: #link("https://www.anthropic.com/research/long-running-Claude")
- Prompt-injection defenses: #link("https://www.anthropic.com/research/prompt-injection-defenses")

== Spring AI


- Reference: #link("https://docs.spring.io/spring-ai/reference/")
- `ChatClient`: #link("https://docs.spring.io/spring-ai/reference/api/chatclient.html")
- Tool calling: #link("https://docs.spring.io/spring-ai/reference/api/tools.html")
- RAG: #link("https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html")
- Vector databases: #link("https://docs.spring.io/spring-ai/reference/api/vectordbs.html")
- Chat memory: #link("https://docs.spring.io/spring-ai/reference/api/chat-memory.html")
- Advisors: #link("https://docs.spring.io/spring-ai/reference/api/advisors.html")
- Observability: #link("https://docs.spring.io/spring-ai/reference/observability/index.html")
- Upgrade notes: #link("https://docs.spring.io/spring-ai/reference/upgrade-notes.html")

== Protocol và orchestration


- MCP specification: #link("https://modelcontextprotocol.io/specification/2025-11-25")
- MCP tools: #link("https://modelcontextprotocol.io/specification/2025-11-25/server/tools")
- LangGraph overview: #link("https://docs.langchain.com/oss/python/langgraph/overview")
- LangGraph graph API: #link("https://docs.langchain.com/oss/python/langgraph/graph-api")
- LangGraph persistence: #link("https://docs.langchain.com/oss/python/langgraph/persistence")
- LangGraph interrupts: #link("https://docs.langchain.com/oss/python/langgraph/interrupts")
- LangGraph fault tolerance: #link("https://docs.langchain.com/oss/python/langgraph/fault-tolerance")

== Data, security và observability


- pgvector: #link("https://github.com/pgvector/pgvector")
- OWASP LLM Top 10 2025: #link("https://genai.owasp.org/llmrisk/llm01-prompt-injection/")
- OWASP LLM Top 10 PDF: #link("https://owasp.org/www-project-top-10-for-large-language-model-applications/assets/PDF/OWASP-Top-10-for-LLMs-v2025.pdf")
- OpenTelemetry GenAI conventions: #link("https://opentelemetry.io/docs/specs/semconv/gen-ai/")
- NIST AI Risk Management Framework: #link("https://www.nist.gov/itl/ai-risk-management-framework")

== Papers nên đọc khi cần hiểu gốc


- Attention Is All You Need (Transformer): #link("https://arxiv.org/abs/1706.03762")
- Retrieval-Augmented Generation (Lewis et al.): #link("https://arxiv.org/abs/2005.11401")
- ReAct: #link("https://arxiv.org/abs/2210.03629")
- Toolformer: #link("https://arxiv.org/abs/2302.04761")
- Self-RAG: #link("https://arxiv.org/abs/2310.11511")
- Generative Agents: #link("https://arxiv.org/abs/2304.03442")

#v(0.5em)
#line(length: 100%, stroke: 0.5pt + rgb("#e2e8f0"))
#v(0.5em)


== 34. Quy tắc cập nhật tài liệu


- #chk Mỗi model project mới: ghi unit of prediction, target schema, split group/time, leakage boundary, baseline và metric trước architecture.
- #chk Mỗi lần đổi dataset/label/preprocessing/augmentation: version manifest và chạy lại data validation, tiny-set, holdout + slice eval.
- #chk Mỗi lần đổi loss/head/num classes/tokenizer/input geometry: kiểm tra lại shape/dtype/label/coordinate contract và inference parity.
- #chk Mỗi lần đổi checkpoint/export/runtime/threshold: đóng gói lại preprocessing + label map và benchmark trên target device.
- #chk Mỗi phần code do AI sinh: phải đi qua quality gate tương ứng; “chạy được” không phải acceptance criterion.
- #chk Mỗi lần đổi model/provider/SDK: pin version và chạy eval gate.
- #chk Mỗi lần đổi eval set/rubric/judge: version manifest, kiểm tra contamination, chấm lại baseline và calibration sample trước khi so candidate.
- #chk Mỗi lần đổi chunking/index/ACL: chạy retrieval + security regression.
- #chk Mỗi lần thêm tool: threat model, typed schema, idempotency và approval review.
- #chk Mỗi lần thêm loop/graph/agent: ghi stop condition, budget, failure matrix và trace.
- #chk Mỗi incident production: thêm một case vào golden/adversarial set và cập nhật runbook.
- #chk Mỗi feature mới: bắt đầu bằng RFC/TDD ngắn với NFR (quality, latency, cost, privacy, availability).
- #chk Mỗi release: lưu rollback bundle gồm prompt/model/index/tool/graph versions.

*Nguyên tắc kết thúc:* Năng lực AI Engineering không được đánh giá bằng số keyword,
framework hay số dòng tự code. Với model project, phải giải thích được *data/split nào,
shape/loss/metric nào, lỗi ở slice nào và artifact được kiểm chứng ra sao*. Với
LLM/Agent system, phải giải thích được *vì sao nó trả lời như vậy, đã dùng dữ liệu/quyền
nào, sẽ dừng ở đâu, thất bại thế nào và khôi phục ra sao*.
