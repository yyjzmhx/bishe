import fitz, os
d = r"C:\Users\晨\AppData\Local\Temp\360zip$Temp\360$6"
files = [f for f in os.listdir(d) if f.endswith(".pdf")]
path = os.path.join(d, files[0])
doc = fitz.open(path)
print(f"Pages: {doc.page_count}")
for i in range(doc.page_count):
    page = doc[i]
    pix = page.get_pixmap(dpi=200)
    out = os.path.join(d, f"page_{i+1}.png")
    pix.save(out)
    print(f"Saved: page_{i+1}.png")
doc.close()
