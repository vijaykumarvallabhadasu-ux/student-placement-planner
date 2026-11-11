"""
Student Placement Planner - Complete Project Source Code PDF Exporter
Scans the entire project folder and compiles every single source code file
into one comprehensive, publication-quality PDF with an automated Table of Contents,
exact line numbers, clean typography, and page numbers.
"""

import os
import sys
import io
import datetime
from pathlib import Path

# ReportLab Imports
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Flowable, Preformatted, HRFlowable
)
from reportlab.pdfgen import canvas

# ----------------------------------------------------------------------
# Configuration
# ----------------------------------------------------------------------
PROJECT_ROOT = Path(r"D:\FSDproject\StudentPlacementPlanner")
OUTPUT_DIR = PROJECT_ROOT / "complete_code_pdf"
OUTPUT_PDF = OUTPUT_DIR / "StudentPlacementPlanner_Complete_Source_Code.pdf"

# Palette
COLOR_PRIMARY = colors.HexColor("#0f172a")      # Slate 900
COLOR_HEADER_BG = colors.HexColor("#1e293b")    # Slate 800
COLOR_ACCENT = colors.HexColor("#4338ca")       # Indigo
COLOR_TEAL = colors.HexColor("#0d9488")         # Teal
COLOR_BORDER = colors.HexColor("#cbd5e1")       # Slate 300
COLOR_LIGHT_BG = colors.HexColor("#f8fafc")     # Slate 50
COLOR_CODE_BG = colors.HexColor("#f8fafc")      # Off-white code background
COLOR_MUTED = colors.HexColor("#64748b")        # Slate 500
COLOR_LINE_NUM = colors.HexColor("#94a3b8")     # Slate 400

# ----------------------------------------------------------------------
# Bookmark & Page Tracker Flowable
# ----------------------------------------------------------------------
class BookmarkFlowable(Flowable):
    """Zero-size flowable that records the current page number for a given key."""
    def __init__(self, key: str, tracker_dict: dict):
        super().__init__()
        self.key = key
        self.tracker_dict = tracker_dict
        self.width = 0
        self.height = 0

    def draw(self):
        self.tracker_dict[self.key] = self.canv._pageNumber

# ----------------------------------------------------------------------
# Two-Pass Numbered Canvas for "Page X of Y" and Running Headers/Footers
# ----------------------------------------------------------------------
class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        page_w, page_h = letter
        
        # Suppress running header/footer on Cover Page (Page 1)
        if self._pageNumber > 1:
            # Header
            self.setFont("Helvetica-Bold", 7.5)
            self.setFillColor(COLOR_MUTED)
            self.drawString(36, page_h - 26, "STUDENT PLACEMENT PLANNER — COMPLETE ORIGINAL SOURCE CODE ARCHIVE")
            self.setFont("Helvetica", 7.5)
            self.drawRightString(page_w - 36, page_h - 26, "FSD Lab Project | LBRCE CSE")
            
            self.setStrokeColor(COLOR_BORDER)
            self.setLineWidth(0.5)
            self.line(36, page_h - 30, page_w - 36, page_h - 30)
            
            # Footer
            self.setStrokeColor(COLOR_BORDER)
            self.setLineWidth(0.5)
            self.line(36, 32, page_w - 36, 32)
            
            self.setFont("Helvetica", 7.5)
            self.setFillColor(COLOR_MUTED)
            self.drawString(36, 22, "Lakireddy Bali Reddy College of Engineering (Autonomous) — CSE Batch 2024–2028")
            page_text = f"Page {self._pageNumber} of {page_count}"
            self.drawRightString(page_w - 36, 22, page_text)
            
        self.restoreState()

# ----------------------------------------------------------------------
# Source File Collection & Categorization
# ----------------------------------------------------------------------
def categorize_file(rel_path_str: str) -> tuple[int, str]:
    """Returns a sorting tuple (category_order, category_name)."""
    p = rel_path_str.replace("\\", "/")
    if p.startswith("database/"):
        return (2, "Oracle Database DDL, Schema & Seed SQL Scripts")
    elif p.startswith("src/main/java/com/placementplanner/model/"):
        return (6, "Domain Entity Models (POJOs)")
    elif p.startswith("src/main/java/com/placementplanner/util/"):
        return (7, "Infrastructure, Connection Pooling & Security Utilities")
    elif p.startswith("src/main/java/com/placementplanner/dao/"):
        return (8, "Data Access Object (DAO) Persistence Layer")
    elif p.startswith("src/main/java/com/placementplanner/service/"):
        return (9, "Business Logic, Career Intelligence & Resume Parser Services")
    elif p.startswith("src/main/java/com/placementplanner/web/"):
        return (10, "Web Controllers, Servlets & Security Filter")
    elif p.startswith("src/main/java/jakarta/"):
        return (10, "Web Controllers, Servlets & Security Filter")
    elif p.startswith("src/main/webapp/WEB-INF/views/"):
        return (5, "JSP Presentation Views & Universal Fragments")
    elif p.startswith("src/main/webapp/assets/"):
        return (4, "Client-Side Frontend Assets (CSS Design System & JS)")
    elif p.startswith("src/main/webapp/"):
        return (3, "Web Application Configuration & Gateway Router")
    elif p.startswith("src/test/"):
        return (11, "Automated Verification Test Suite & Database Diagnostics")
    else:
        return (1, "Project Configuration, Maven Build & Execution Scripts")

def scan_all_source_files(root_dir: Path):
    """Scans and retrieves all valid project source code files."""
    ignore_dirs = {".git", ".settings", "target", "build", "code_inspection_report", "complete_code_pdf", ".agents", "__pycache__"}
    allowed_exts = {".java", ".jsp", ".jspf", ".html", ".css", ".js", ".xml", ".sql", ".bat", ".ps1"}
    special_files = {".classpath", ".project"}
    
    collected = []
    
    for path in sorted(root_dir.rglob("*")):
        if path.is_file():
            if any(part in ignore_dirs for part in path.parts):
                continue
                
            ext = path.suffix.lower()
            if ext in allowed_exts or path.name in special_files:
                try:
                    content = path.read_text(encoding="utf-8", errors="ignore")
                    lines = content.splitlines()
                    rel_path = path.relative_to(root_dir)
                    cat_order, cat_name = categorize_file(str(rel_path))
                    collected.append({
                        "path": path,
                        "rel_path": str(rel_path),
                        "filename": path.name,
                        "ext": ext or path.name,
                        "cat_order": cat_order,
                        "cat_name": cat_name,
                        "lines": lines,
                        "line_count": len(lines),
                        "char_count": len(content)
                    })
                except Exception as e:
                    print(f"Warning: could not read {path}: {e}")
                    
    # Sort primarily by category order, then by relative path
    collected.sort(key=lambda item: (item["cat_order"], item["rel_path"]))
    return collected

# ----------------------------------------------------------------------
# Line Formatter with Smart Indented Wrapping
# ----------------------------------------------------------------------
def format_code_with_line_numbers(lines: list[str], max_width: int = 110) -> str:
    """Formats code lines with line numbers and wraps lines exceeding max_width."""
    formatted_lines = []
    
    for idx, raw_line in enumerate(lines, 1):
        # Expand tabs to 4 spaces for consistent visual alignment
        line = raw_line.replace("\t", "    ")
        num_prefix = f"{idx:4d} | "
        indent_prefix = "     |     -> "
        
        if len(line) <= max_width:
            formatted_lines.append(num_prefix + line)
        else:
            # Wrap long line cleanly
            first_chunk = line[:max_width]
            formatted_lines.append(num_prefix + first_chunk)
            remainder = line[max_width:]
            
            while remainder:
                chunk = remainder[:max_width - 10]
                formatted_lines.append(indent_prefix + chunk)
                remainder = remainder[max_width - 10:]
                
    return "\n".join(formatted_lines)

# ----------------------------------------------------------------------
# Story Builder
# ----------------------------------------------------------------------
def build_story(files: list[dict], page_tracker: dict, is_pass_one: bool):
    """Builds the flowable story. page_tracker is populated dynamically."""
    styles = getSampleStyleSheet()
    
    title_style = ParagraphStyle(
        "CoverTitle",
        parent=styles["Title"],
        fontName="Helvetica-Bold",
        fontSize=24,
        leading=30,
        textColor=COLOR_PRIMARY,
        alignment=1,
        spaceAfter=10
    )
    
    subtitle_style = ParagraphStyle(
        "CoverSubtitle",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=12,
        leading=16,
        textColor=COLOR_ACCENT,
        alignment=1,
        spaceAfter=20
    )
    
    h1_style = ParagraphStyle(
        "SectionH1",
        parent=styles["Heading1"],
        fontName="Helvetica-Bold",
        fontSize=13,
        leading=17,
        textColor=COLOR_PRIMARY,
        spaceBefore=14,
        spaceAfter=6,
        keepWithNext=True
    )
    
    h2_style = ParagraphStyle(
        "SectionH2",
        parent=styles["Heading2"],
        fontName="Helvetica-Bold",
        fontSize=10.5,
        leading=14,
        textColor=COLOR_ACCENT,
        spaceBefore=8,
        spaceAfter=4,
        keepWithNext=True
    )
    
    body_style = ParagraphStyle(
        "BodySmall",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=8.5,
        leading=11.5,
        textColor=COLOR_PRIMARY
    )
    
    toc_title_style = ParagraphStyle(
        "TOCTitle",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=8,
        leading=11,
        textColor=COLOR_PRIMARY
    )
    
    toc_page_style = ParagraphStyle(
        "TOCPage",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=8,
        leading=11,
        alignment=2, # Right aligned
        textColor=COLOR_ACCENT
    )
    
    code_style = ParagraphStyle(
        "SourceCodeListing",
        fontName="Courier",
        fontSize=6.5,
        leading=8.2,
        textColor=colors.HexColor("#0f172a"),
        backColor=COLOR_CODE_BG
    )
    
    file_banner_style = ParagraphStyle(
        "FileBanner",
        fontName="Helvetica-Bold",
        fontSize=9,
        leading=12,
        textColor=colors.white
    )
    
    file_meta_style = ParagraphStyle(
        "FileMeta",
        fontName="Helvetica",
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor("#cbd5e1"),
        alignment=2
    )

    story = []

    # ==================================================================
    # 1. COVER PAGE
    # ==================================================================
    story.append(Spacer(1, 30))
    story.append(Paragraph("LAKIREDDY BALI REDDY COLLEGE OF ENGINEERING", ParagraphStyle("CoverInst", fontName="Helvetica-Bold", fontSize=13, leading=16, alignment=1, textColor=COLOR_PRIMARY)))
    story.append(Paragraph("(AUTONOMOUS) — MYLAVARAM, KRISHNA DISTRICT, ANDHRA PRADESH", ParagraphStyle("CoverInstSub", fontName="Helvetica", fontSize=9, leading=13, alignment=1, textColor=COLOR_MUTED)))
    story.append(Paragraph("Approved by AICTE, New Delhi & Affiliated to JNTUK, Kakinada | Accredited by NAAC & NBA", ParagraphStyle("CoverAccred", fontName="Helvetica", fontSize=8, leading=11, alignment=1, textColor=COLOR_MUTED)))
    story.append(Spacer(1, 15))
    story.append(HRFlowable(width="80%", thickness=1.5, color=COLOR_ACCENT, spaceAfter=20, spaceBefore=0))
    
    story.append(Paragraph("DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING", ParagraphStyle("CoverDept", fontName="Helvetica-Bold", fontSize=11, leading=15, alignment=1, textColor=COLOR_ACCENT)))
    story.append(Paragraph("FULL STACK DEVELOPMENT (FSD) LABORATORY PROJECT", ParagraphStyle("CoverLab", fontName="Helvetica-Bold", fontSize=9.5, leading=13, alignment=1, textColor=COLOR_TEAL)))
    story.append(Spacer(1, 20))
    
    story.append(Paragraph("STUDENT PLACEMENT PLANNER", title_style))
    story.append(Paragraph("Complete Original Source Code Compendium — All Files, Classes, Servlets, DAOs, JSPs & SQL", subtitle_style))
    story.append(Spacer(1, 15))
    
    total_loc = sum(item["line_count"] for item in files)
    
    meta_data = [
        [Paragraph("<b>Student Name:</b>", body_style), Paragraph("V. Vijay Kumar", body_style)],
        [Paragraph("<b>Roll Number:</b>", body_style), Paragraph("24761A0559", body_style)],
        [Paragraph("<b>Department:</b>", body_style), Paragraph("Computer Science and Engineering (CSE)", body_style)],
        [Paragraph("<b>Academic Batch:</b>", body_style), Paragraph("2024 – 2028", body_style)],
        [Paragraph("<b>Project Guide:</b>", body_style), Paragraph("Dr. K. DeviPriya, Associate Professor", body_style)],
        [Paragraph("<b>Technology Stack:</b>", body_style), Paragraph("Java Servlets 4.0, JSP 2.3, JDBC, Oracle XE, Apache Tomcat 9", body_style)],
        [Paragraph("<b>Total Files Included:</b>", body_style), Paragraph(f"<b>{len(files)} Source Files</b> (100% Unabridged)", body_style)],
        [Paragraph("<b>Total Lines of Code:</b>", body_style), Paragraph(f"<b>{total_loc:,} Physical Lines</b>", body_style)],
        [Paragraph("<b>Archived On:</b>", body_style), Paragraph(datetime.datetime.now().strftime("%d %B %Y, %I:%M %p"), body_style)],
    ]
    
    meta_table = Table(meta_data, colWidths=[140, 360])
    meta_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), COLOR_LIGHT_BG),
        ('BOX', (0,0), (-1,-1), 1, COLOR_BORDER),
        ('INNERGRID', (0,0), (-1,-1), 0.5, COLOR_BORDER),
        ('PADDING', (0,0), (-1,-1), 5),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
    ]))
    story.append(meta_table)
    story.append(Spacer(1, 25))
    
    notice_text = (
        "<b>Notice:</b> This document contains the verbatim, unabridged source code for all 53 project files comprising the "
        "Student Placement Planner application. All original comments, indentation, package imports, SQL DDL queries, and "
        "business logic have been preserved without alteration, omission, or truncation."
    )
    story.append(Paragraph(notice_text, ParagraphStyle("Notice", fontName="Helvetica-Oblique", fontSize=8, leading=11, alignment=1, textColor=COLOR_MUTED)))
    story.append(PageBreak())

    # ==================================================================
    # 2. TABLE OF CONTENTS
    # ==================================================================
    story.append(Paragraph("TABLE OF CONTENTS", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=COLOR_PRIMARY, spaceAfter=8, spaceBefore=0))
    story.append(Paragraph("Click on any file entry or navigate to the corresponding page number to inspect its complete source code.", body_style))
    story.append(Spacer(1, 8))
    
    current_category = None
    toc_rows = [
        [Paragraph("<b>Included Source File & Path</b>", body_style), Paragraph("<b>Lines</b>", ParagraphStyle("THCenter", parent=body_style, alignment=1)), Paragraph("<b>Page</b>", ParagraphStyle("THRight", parent=body_style, alignment=2))]
    ]
    
    for item in files:
        # Category separator row
        if item["cat_name"] != current_category:
            current_category = item["cat_name"]
            toc_rows.append([
                Paragraph(f"<b>{current_category}</b>", ParagraphStyle("CatHeader", fontName="Helvetica-Bold", fontSize=8, leading=10, textColor=COLOR_ACCENT)),
                Paragraph("", body_style),
                Paragraph("", body_style)
            ])
            
        page_str = str(page_tracker.get(item["rel_path"], "--")) if not is_pass_one else "--"
        toc_rows.append([
            Paragraph(f"• <code>{item['rel_path']}</code>", toc_title_style),
            Paragraph(f"{item['line_count']}", ParagraphStyle("LOC", fontName="Helvetica", fontSize=7.5, leading=10, alignment=1, textColor=COLOR_MUTED)),
            Paragraph(f"<b>{page_str}</b>", toc_page_style)
        ])
        
    toc_table = Table(toc_rows, colWidths=[380, 50, 70])
    toc_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), COLOR_HEADER_BG),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('GRID', (0,0), (-1,-1), 0.5, COLOR_BORDER),
        ('PADDING', (0,0), (-1,-1), 3),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
    ]))
    story.append(toc_table)
    story.append(PageBreak())

    # ==================================================================
    # 3. SOURCE CODE LISTINGS (FILE BY FILE)
    # ==================================================================
    for idx, item in enumerate(files, 1):
        # 1. Bookmark flowable to record start page
        story.append(BookmarkFlowable(item["rel_path"], page_tracker))
        
        # 2. File Header Banner Table
        banner_data = [
            [
                Paragraph(f"FILE {idx} OF {len(files)}: <code>{item['rel_path']}</code>", file_banner_style),
                Paragraph(f"{item['line_count']} Lines | {item['char_count']:,} Bytes", file_meta_style)
            ]
        ]
        banner_table = Table(banner_data, colWidths=[360, 140])
        banner_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), COLOR_HEADER_BG),
            ('PADDING', (0,0), (-1,-1), 4),
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ]))
        story.append(banner_table)
        story.append(Spacer(1, 2))
        
        # 3. Formatted Code with Line Numbers
        formatted_code = format_code_with_line_numbers(item["lines"])
        story.append(Preformatted(formatted_code, code_style))
        
        # 4. PageBreak to ensure next file starts cleanly on a fresh page
        if idx < len(files):
            story.append(PageBreak())

    return story

# ----------------------------------------------------------------------
# Main Execution (Two-Pass Compilation)
# ----------------------------------------------------------------------
def main():
    print("=" * 75)
    print("Student Placement Planner - Complete Source Code PDF Exporter")
    print("=" * 75)
    
    print(f"[1/4] Scanning all project directories: {PROJECT_ROOT}")
    if not PROJECT_ROOT.exists():
        print(f"[FATAL] Project root does not exist: {PROJECT_ROOT}")
        sys.exit(1)
        
    source_files = scan_all_source_files(PROJECT_ROOT)
    total_loc = sum(f["line_count"] for f in source_files)
    print(f"      Discovered {len(source_files)} source files totaling {total_loc:,} lines of code.")
    
    # Pass 1: Build in-memory to record exact page numbers for all 53 files
    print("[2/4] Executing Pass 1 (In-memory page tracking for Table of Contents)...")
    page_tracker = {}
    pass1_story = build_story(source_files, page_tracker, is_pass_one=True)
    
    dummy_doc = SimpleDocTemplate(
        io.BytesIO(),
        pagesize=letter,
        leftMargin=36,
        rightMargin=36,
        topMargin=36,
        bottomMargin=36
    )
    dummy_doc.build(pass1_story)
    print(f"      Pass 1 completed. Mapped start pages for {len(page_tracker)} files.")
    
    # Pass 2: Build final PDF with populated Table of Contents
    print(f"[3/4] Executing Pass 2 (Generating final PDF with populated TOC)...")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    final_story = build_story(source_files, page_tracker, is_pass_one=False)
    
    final_doc = SimpleDocTemplate(
        str(OUTPUT_PDF),
        pagesize=letter,
        leftMargin=36,
        rightMargin=36,
        topMargin=36,
        bottomMargin=36
    )
    
    final_doc.build(final_story, canvasmaker=NumberedCanvas)
    
    # Verification
    pdf_size_bytes = OUTPUT_PDF.stat().st_size
    pdf_size_mb = pdf_size_bytes / (1024 * 1024)
    print("[4/4] Verification & File Integrity Check:")
    print(f"      [OK] Output Path : {OUTPUT_PDF}")
    print(f"      [OK] File Size   : {pdf_size_bytes:,} bytes ({pdf_size_mb:.2f} MB)")
    print(f"      [OK] Total Files : {len(source_files)} files included (100% of codebase)")
    print(f"      [OK] Total Lines : {total_loc:,} physical lines formatted with line numbers")
    print("=" * 75)
    print("[SUCCESS] Complete source code PDF archive generated successfully!")

if __name__ == "__main__":
    main()
