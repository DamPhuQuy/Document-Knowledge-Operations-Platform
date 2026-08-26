// ==============================================================================
// PBL REPORT TEMPLATE (Khoa CNTT - Trường ĐH Bách Khoa, ĐH Đà Nẵng)
// ==============================================================================

// Hàm tạo chương không đánh số xuất hiện trong Mục lục (Giới thiệu, Kết luận, Tài liệu tham khảo, Phụ lục...)
#let unnumbered-chapter(title, outlined: true) = {
  heading(level: 1, numbering: none, outlined: outlined)[#title]
}

// Hàm Callout / Hộp ghi chú trực quan
#let note(body) = block(
  fill: rgb("#f0f7ff"),
  stroke: (left: 3pt + rgb("#0066cc")),
  inset: (x: 12pt, y: 10pt),
  radius: (right: 4pt),
  spacing: 1.2em,
  width: 100%,
)[
  #text(weight: "bold", fill: rgb("#0066cc"))[ℹ Ghi chú: ]
  #body
]

#let warning(body) = block(
  fill: rgb("#fff8f0"),
  stroke: (left: 3pt + rgb("#ff9900")),
  inset: (x: 12pt, y: 10pt),
  radius: (right: 4pt),
  spacing: 1.2em,
  width: 100%,
)[
  #text(weight: "bold", fill: rgb("#cc7a00"))[⚠ Chú ý: ]
  #body
]

// Hàm khởi tạo template chính
#let report(
  title: "Tên đề tài báo cáo",
  project_code: "PBL4: DỰ ÁN HỆ ĐIỀU HÀNH & MẠNG MÁY TÍNH",
  topic_code: "Đề tài",
  students: (
    (name: "Sinh viên 1", class: "24T_DT2", group: "24.xx"),
    (name: "Sinh viên 2", class: "24T_DT2", group: "24.xx"),
  ),
  supervisor: "TS. Giảng Viên Hướng Dẫn",
  location: "Đà Nẵng",
  year: "2026",
  font_family: ("New Computer Modern", "Times New Roman", "Liberation Serif"),
  code_font: ("JetBrains Mono", "DejaVu Sans Mono", "Liberation Mono"),
  font_size: 13pt,
  toc_depth: 3,
  abbreviations: (),
  body
) = {
  // ---------------------------------------------------------------------------
  // 1. THIẾT LẬP TRANG VÀ FONT CHỮ CHUẨN VĂN BẢN HỌC THUẬT
  // ---------------------------------------------------------------------------
  set document(title: title, author: students.map(s => s.name))
  set text(
    font: font_family,
    size: font_size,
    lang: "vi",
  )

  // Căn lề chuẩn A4: Trái 3.0cm (đóng gáy), Phải 2.0cm, Trên 2.0cm, Dưới 2.0cm
  set page(
    paper: "a4",
    margin: (top: 2.0cm, bottom: 2.0cm, left: 3.0cm, right: 2.0cm),
  )

  // Đoạn văn: Căn đều 2 bên, thụt đầu dòng 1cm, giãn dòng 1.3
  set par(
    justify: true,
    leading: 0.8em,
    first-line-indent: 1.0cm,
    spacing: 1.2em,
  )

  // Danh sách không thứ tự & có thứ tự
  set list(indent: 0.5cm, body-indent: 0.3cm, spacing: 0.8em)
  set enum(indent: 0.5cm, body-indent: 0.3cm, spacing: 0.8em)

  // Bảng biểu & Hình ảnh
  show figure.where(kind: image): set figure(supplement: [Hình])
  show figure.where(kind: table): set figure(supplement: [Bảng])
  show figure.where(kind: raw): set figure(supplement: [Mã nguồn])

  show figure.caption: it => [
    #text(size: 11pt, style: "italic")[#it]
  ]

  // Bảng biểu: Caption hiển thị bên dưới bảng, căn lề chuẩn, không giãn dòng quá mức
  show figure.where(kind: table): set figure.caption(position: bottom)
  show table: set par(justify: false, first-line-indent: 0pt, leading: 0.65em)
  show table: set text(size: 11pt)
  show table.cell.where(y: 0): set text(weight: "bold")
  set table(
    stroke: 0.5pt + rgb("99a4b3"),
    fill: (col, row) => if row == 0 { rgb("f0f4f8") } else { none },
    inset: (x: 9pt, y: 8pt),
  )

  // Khối mã nguồn (Code Block)
  show raw.where(block: true): it => block(
    fill: rgb("#f8f9fa"),
    stroke: 0.5pt + rgb("#e1e4e8"),
    inset: 10pt,
    radius: 4pt,
    width: 100%,
    spacing: 1.2em,
  )[
    #set text(font: code_font, size: 10pt)
    #set par(justify: false, first-line-indent: 0pt)
    #it
  ]

  // Công thức toán học có đánh số
  set math.equation(numbering: "(1)")

  // ---------------------------------------------------------------------------
  // 2. TRANG BÌA (COVER PAGE)
  // ---------------------------------------------------------------------------
  {
    set page(header: none, footer: none, margin: (top: 2.0cm, bottom: 2.0cm, left: 3.0cm, right: 2.0cm))

    // Viền đôi trang bìa chuẩn đồ án Bách Khoa
    rect(
      width: 100%,
      height: 100%,
      stroke: 2pt + rgb("000000"),
      inset: 4pt,
      outset: 0pt,
      radius: 0pt,
    )[
      #rect(
        width: 100%,
        height: 100%,
        stroke: 0.75pt + rgb("000000"),
        inset: 16pt,
      )[
        #set align(center)
        #text(size: 13.5pt, weight: "bold")[ĐẠI HỌC ĐÀ NẴNG]\
        #text(size: 13.5pt, weight: "bold")[TRƯỜNG ĐẠI HỌC BÁCH KHOA]\
        #text(size: 13pt, weight: "bold")[KHOA CÔNG NGHỆ THÔNG TIN]

        #v(1.2fr)

        #image("logo_dut.png", width: 3.2cm)

        #v(1.2fr)

        #text(size: 14pt, weight: "bold")[#project_code]

        #v(0.8fr)

        #block(width: 96%)[
          #set text(size: 13.5pt, weight: "bold", hyphenate: false)
          #set par(justify: false, leading: 0.7em)
          #topic_code: #title
        ]

        #v(1.5fr)

        #align(center)[
          #block(width: 82%)[
            #set align(left)
            #text(size: 12.5pt, weight: "bold")[SINH VIÊN THỰC HIỆN:]
            #v(0.3cm)
            #grid(
              columns: (1fr, auto) + if students.any(s => "group" in s and s.group != "") { (auto,) } else { () },
              column-gutter: 1.5cm,
              row-gutter: 0.4cm,
              ..students.map(s => {
                let cells = (
                  text(size: 12.5pt, weight: "bold")[#s.name],
                  text(size: 12.5pt)[#if "class" in s and s.class != "" [LỚP: *#s.class*]],
                )
                if students.any(st => "group" in st and st.group != "") {
                  cells.push(text(size: 12.5pt)[#if "group" in s and s.group != "" [NHÓM: *#s.group*]])
                }
                cells
              }).flatten()
            )
            #v(0.6cm)
            #text(size: 12.5pt)[*GIẢNG VIÊN HƯỚNG DẪN:* #supervisor]
          ]
        ]

        #v(2fr)

        #text(size: 12.5pt, weight: "bold")[#location, #year]
        #v(0.2fr)
      ]
    ]
  }

  pagebreak()

  // ---------------------------------------------------------------------------
  // 3. THIẾT LẬP ĐÁNH SỐ TRANG THỐNG NHẤT (SỐ Ả RẬP: 1, 2, 3, 4, 5...)
  // ---------------------------------------------------------------------------
  counter(page).update(1)

  set page(
    header: context {
      align(center)[
        #text(size: 10pt, style: "italic")[#project_code]
        #line(length: 100%, stroke: 0.5pt)
      ]
    },
    footer: context {
      let page_num = numbering("1", counter(page).get().first())
      [
        #line(length: 100%, stroke: 0.5pt)
        #grid(
          columns: (1fr, 1fr),
          align(left)[
            #text(size: 10pt, style: "italic")[
              #students.map(s => s.name).join(", ")
            ]
          ],
          align(right)[
            #text(size: 10pt, style: "italic")[Trang #page_num]
          ]
        )
      ]
    },
  )

  // Cấu hình quy tắc tự động định dạng dòng Mục lục (TOC styling)
  show outline.entry.where(level: 1): it => {
    v(10pt, weak: true)
    strong(it)
  }

  // Tự động tạo Mục lục (Table of Contents)
  [
    #show heading: set align(center)
    #show heading: set text(size: 14pt, weight: "bold")
    #outline(
      title: "MỤC LỤC",
      indent: auto,
      depth: toc_depth,
    )
  ]

  pagebreak()

  // Danh sách hình vẽ (Tự động cập nhật từ tất cả figure image)
  [
    #show heading: set align(center)
    #show heading: set text(size: 14pt, weight: "bold")
    #outline(
      title: "DANH SÁCH HÌNH VẼ",
      target: figure.where(kind: image),
    )
  ]

  pagebreak()

  // Danh sách bảng biểu (Tự động cập nhật từ tất cả figure table)
  [
    #show heading: set align(center)
    #show heading: set text(size: 14pt, weight: "bold")
    #outline(
      title: "DANH SÁCH BẢNG BIỂU",
      target: figure.where(kind: table),
    )
  ]

  // Bảng từ viết tắt (nếu có)
  if abbreviations.len() > 0 [
    #pagebreak()
    #align(center)[
      #text(size: 14pt, weight: "bold")[DANH SÁCH CÁC TỪ VIẾT TẮT]
    ]
    #v(1cm)
    #table(
      columns: (1.8fr, 4.2fr, 4fr),
      align: (center + horizon, left + horizon, left + horizon),
      table.header(
        [*Từ viết tắt*], [*Thuật ngữ tiếng Anh*], [*Ý nghĩa / Giải thích*]
      ),
      ..abbreviations.map(item => (
        [#item.at(0)],
        [#item.at(1)],
        [#item.at(2)],
      )).flatten()
    )
  ]

  pagebreak()

  // ---------------------------------------------------------------------------
  // 4. THIẾT LẬP QUY TẮC ĐÁNH SỐ HEADING CHO NỘI DUNG
  // ---------------------------------------------------------------------------

  // Cấu hình quy tắc đánh số Heading tự động:
  // Level 1: CHƯƠNG 1. TÊN CHƯƠNG (ngắt trang tự động)
  // Level 2: 1.1. Tiêu đề mục
  // Level 3: 1.1.1. Tiêu đề mục con
  set heading(numbering: (..nums) => {
    let list = nums.pos()
    if list.len() == 1 {
      return "CHƯƠNG " + str(list.at(0)) + ". "
    } else {
      return list.map(str).join(".") + ". "
    }
  })

  // Show rules định dạng giao diện tiêu đề
  show heading.where(level: 1): it => [
    #pagebreak(weak: true)
    #v(0.5cm)
    #text(size: 14pt, weight: "bold")[#it]
    #v(0.5cm)
  ]

  show heading.where(level: 2): it => [
    #v(0.3cm)
    #text(size: 13pt, weight: "bold")[#it]
    #v(0.2cm)
  ]

  show heading.where(level: 3): it => [
    #v(0.2cm)
    #text(size: 13pt, weight: "bold", style: "italic")[#it]
    #v(0.1cm)
  ]

  // Render toàn bộ nội dung người dùng viết
  body
}
