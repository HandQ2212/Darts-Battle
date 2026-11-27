import re
TOK=re.compile(r'<(/?)w:(p|tbl)(?=[\s>/])([^>]*?)(/?)>')
def split_body(x):
    b0=x.index('<w:body>')+8; b1=x.rindex('</w:body>')
    body=x[b0:b1]
    els=[];depth=0;start=None
    for m in TOK.finditer(body):
        close,selfclose=m.group(1),m.group(4)
        if selfclose:
            if depth==0: els.append(body[m.start():m.end()])
            continue
        if not close:
            if depth==0: start=m.start()
            depth+=1
        else:
            depth-=1
            if depth==0: els.append(body[start:m.end()])
    sect=body[body.rindex('<w:sectPr'):]
    return x[:b0], els, sect, x[b1:]
