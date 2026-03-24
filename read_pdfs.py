import PyPDF2
import os

pdf_files = [
    r'd:\study\front-end\Myblog\.trae\specs\工业级智能体记忆系统开发实践(1).pdf',
    r'd:\study\front-end\Myblog\.trae\specs\Mini-OpenClaw README.pdf',
    r'd:\study\front-end\Myblog\.trae\specs\Mini-OpenClaw 开发需求文档 (PRD).pdf'
]

for pdf_path in pdf_files:
    if os.path.exists(pdf_path):
        print(f'\n=== {os.path.basename(pdf_path)} ===')
        try:
            with open(pdf_path, 'rb') as f:
                reader = PyPDF2.PdfReader(f)
                print(f'Total pages: {len(reader.pages)}')
                for i, page in enumerate(reader.pages[:5]):  # 只读前5页
                    text = page.extract_text()
                    print(f'\n--- Page {i+1} ---')
                    if text:
                        print(text[:3000])
                    else:
                        print('No text extracted')
        except Exception as e:
            print(f'Error: {e}')
    else:
        print(f'File not found: {pdf_path}')
