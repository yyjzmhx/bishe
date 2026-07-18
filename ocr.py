import fitz
doc = fitz.open(r"C:\Users\晨\Desktop\2022级软件件工程专业\8.叶江峰--毕业设计（论文）指导记录表.pdf")
for i in range(doc.page_count):
    page = doc[i]
    text = page.get_text("text")
    if text.strip():
        print(f"=== Page {i+1} (fitz) ===")
        print(text[:3000])
    else:
        print(f"Page {i+1}: No text layer")
doc.close()
