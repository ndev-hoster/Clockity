#!/usr/bin/env python3
import os
import sys
import math
import subprocess
import argparse

OUTPUT_DIR = "/home/kratoes/Downloads/clockity_icons"
RES_DIR = "/home/kratoes/Projects/clockity/app/src/main/res"

# Color Palette Constants
ONE_UI_BLACK = "#000000"
ONE_UI_SURFACE = "#121214"
ONE_UI_CARD = "#1C1C1E"
ONE_UI_DIVIDER = "#2C2C2E"
ONE_UI_BLUE = "#3E82F7"
ONE_UI_BLUE_LIGHT = "#64B5F6"
ONE_UI_YELLOW = "#FFD60A"
ONE_UI_RED = "#FF453A"
ONE_UI_WHITE = "#FFFFFF"

def get_squircle_path(cx=256, cy=256, r=220, roundness=90):
    """Generates an ultra-smooth One UI style squircle path."""
    x0, y0 = cx - r, cy - r
    w, h = r * 2, r * 2
    return f"""<rect x="{x0}" y="{y0}" width="{w}" height="{h}" rx="{roundness}" ry="{roundness}" fill="{ONE_UI_BLACK}" stroke="{ONE_UI_DIVIDER}" stroke-width="2" />"""

def generate_minimal_glyph_svg(bg_color=ONE_UI_BLACK, squircle=True):
    """Concept C: 3-Color Minimalist Glyph (Blue Orbit, White/Yellow Hands, Red 12 o'clock Pip)."""
    cx, cy = 256, 256
    radius = 145

    # Hand coordinates (10:10 aesthetic)
    # Hour hand (10 o'clock -> ~300 deg, len=65)
    hour_ang_rad = math.radians(305)
    hx = cx + 65 * math.sin(hour_ang_rad)
    hy = cy - 65 * math.cos(hour_ang_rad)

    # Minute hand (2 o'clock -> ~55 deg, len=100)
    min_ang_rad = math.radians(55)
    mx = cx + 100 * math.sin(min_ang_rad)
    my = cy - 100 * math.cos(min_ang_rad)

    bg_shape = get_squircle_path(cx, cy, 230, 95) if squircle else f'<rect width="512" height="512" fill="{bg_color}" />'

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
    <defs>
        <filter id="blue-glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="8" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
        <filter id="red-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="5" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
    </defs>

    <!-- Background -->
    {bg_shape}

    <!-- Dial Background Plate -->
    <circle cx="{cx}" cy="{cy}" r="{radius + 15}" fill="#0A0A0C" stroke="{ONE_UI_DIVIDER}" stroke-width="1.5" />

    <!-- Orbital Glow Ring (One UI Blue) -->
    <circle cx="{cx}" cy="{cy}" r="{radius}" fill="none" stroke="{ONE_UI_BLUE}" stroke-width="5.5" filter="url(#blue-glow)" />

    <!-- 12 o'clock Alarm Indicator Dot (One UI Red) -->
    <circle cx="{cx}" cy="{cy - radius}" r="9" fill="{ONE_UI_RED}" filter="url(#red-glow)" />
    <circle cx="{cx}" cy="{cy - radius}" r="4" fill="#FFA39E" />

    <!-- 3, 6, 9 o'clock Subtle Pips -->
    <circle cx="{cx + radius}" cy="{cy}" r="3.5" fill="{ONE_UI_BLUE_LIGHT}" opacity="0.6" />
    <circle cx="{cx}" cy="{cy + radius}" r="3.5" fill="{ONE_UI_BLUE_LIGHT}" opacity="0.6" />
    <circle cx="{cx - radius}" cy="{cy}" r="3.5" fill="{ONE_UI_BLUE_LIGHT}" opacity="0.6" />

    <!-- Hour Hand (Minimal White) -->
    <line x1="{cx}" y1="{cy}" x2="{hx}" y2="{hy}" stroke="{ONE_UI_WHITE}" stroke-width="10" stroke-linecap="round" />

    <!-- Minute Hand (One UI Yellow) -->
    <line x1="{cx}" y1="{cy}" x2="{mx}" y2="{my}" stroke="{ONE_UI_YELLOW}" stroke-width="8" stroke-linecap="round" />

    <!-- Center Pivot -->
    <circle cx="{cx}" cy="{cy}" r="11" fill="#18181B" stroke="{ONE_UI_WHITE}" stroke-width="4" />
    <circle cx="{cx}" cy="{cy}" r="3" fill="{ONE_UI_YELLOW}" />
</svg>"""
    return svg

def generate_chrono_ticks_svg(squircle=True):
    """Concept A: The Chrono Dial with Radial Ticks and Luminous Outer Track."""
    cx, cy = 256, 256
    r_outer = 150
    r_inner = 135

    ticks = []
    for i in range(60):
        deg = i * 6
        rad = math.radians(deg)
        is_major = (i % 5 == 0)
        len_tick = 14 if is_major else 6
        w = 3.5 if is_major else 1.5
        color = ONE_UI_YELLOW if i == 15 else (ONE_UI_WHITE if is_major else "#3A3A3E")

        x1 = cx + (r_outer - 5) * math.sin(rad)
        y1 = cy - (r_outer - 5) * math.cos(rad)
        x2 = cx + (r_outer - 5 - len_tick) * math.sin(rad)
        y2 = cy - (r_outer - 5 - len_tick) * math.cos(rad)
        ticks.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{color}" stroke-width="{w}" stroke-linecap="round" />')

    ticks_svg = "\n    ".join(ticks)

    # 10:10 hands
    hour_ang_rad = math.radians(308)
    hx = cx + 70 * math.sin(hour_ang_rad)
    hy = cy - 70 * math.cos(hour_ang_rad)

    min_ang_rad = math.radians(52)
    mx = cx + 108 * math.sin(min_ang_rad)
    my = cy - 108 * math.cos(min_ang_rad)

    bg_shape = get_squircle_path(cx, cy, 230, 95) if squircle else '<rect width="512" height="512" fill="#000000" />'

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
    <defs>
        <radialGradient id="chrono-plate" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#141416" />
            <stop offset="100%" stop-color="#070708" />
        </radialGradient>
        <filter id="chrono-glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="6" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
    </defs>

    {bg_shape}

    <!-- Dial Plate -->
    <circle cx="{cx}" cy="{cy}" r="{r_outer + 8}" fill="url(#chrono-plate)" stroke="{ONE_UI_DIVIDER}" stroke-width="2" />
    <circle cx="{cx}" cy="{cy}" r="{r_outer}" fill="none" stroke="{ONE_UI_BLUE}" stroke-width="4.5" filter="url(#chrono-glow)" />

    <!-- Radial Tick Marks -->
    {ticks_svg}

    <!-- Hands -->
    <line x1="{cx}" y1="{cy}" x2="{hx:.1f}" y2="{hy:.1f}" stroke="{ONE_UI_WHITE}" stroke-width="9" stroke-linecap="round" />
    <line x1="{cx}" y1="{cy}" x2="{mx:.1f}" y2="{my:.1f}" stroke="{ONE_UI_BLUE_LIGHT}" stroke-width="7" stroke-linecap="round" />

    <!-- Center Hub -->
    <circle cx="{cx}" cy="{cy}" r="12" fill="#202024" stroke="{ONE_UI_WHITE}" stroke-width="3.5" />
    <circle cx="{cx}" cy="{cy}" r="4" fill="{ONE_UI_YELLOW}" />
</svg>"""
    return svg

def generate_action_stopwatch_svg(squircle=True):
    """Concept B: Tactile Action Stopwatch with Pushers and Dual-color Split, perfectly centered."""
    cx = 256
    cy = 265  # Perfectly optical-centered accounting for top crown (256 - (292/2) = 110, 256 + (292/2) = 402)
    r = 125
    r_outer = 137

    # 10:10 hands
    hour_ang = math.radians(305)
    hx = cx + 58 * math.sin(hour_ang)
    hy = cy - 58 * math.cos(hour_ang)

    min_ang = math.radians(55)
    mx = cx + 88 * math.sin(min_ang)
    my = cy - 88 * math.cos(min_ang)

    bg_shape = get_squircle_path(256, 256, 230, 95) if squircle else '<rect width="512" height="512" fill="#000000" />'

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
    <defs>
        <filter id="sw-glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="5" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
        <radialGradient id="sw-inner-plate" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#121216" />
            <stop offset="100%" stop-color="#050507" />
        </radialGradient>
    </defs>

    {bg_shape}

    <!-- Top Crown Base Stem -->
    <rect x="{cx - 7}" y="{cy - r_outer - 8}" width="14" height="10" fill="{ONE_UI_BLUE_LIGHT}" />

    <!-- Top Crown Button Head -->
    <rect x="{cx - 16}" y="{cy - r_outer - 22}" width="32" height="16" rx="5" fill="{ONE_UI_BLUE}" />

    <!-- Right Side Reset Button (45 deg) -->
    <g transform="rotate(45 {cx} {cy})">
        <rect x="{cx - 8}" y="{cy - r_outer - 12}" width="16" height="14" rx="4" fill="{ONE_UI_BLUE}" />
    </g>

    <!-- Stopwatch Outer Ring Chassis (One UI Blue) -->
    <circle cx="{cx}" cy="{cy}" r="{r_outer}" fill="#0A0A0E" stroke="{ONE_UI_BLUE}" stroke-width="12" />

    <!-- Inner Dial Plate -->
    <circle cx="{cx}" cy="{cy}" r="{r}" fill="url(#sw-inner-plate)" stroke="{ONE_UI_DIVIDER}" stroke-width="1.5" />

    <!-- Subtle Dial Accent Ring -->
    <circle cx="{cx}" cy="{cy}" r="{r - 18}" fill="none" stroke="{ONE_UI_DIVIDER}" stroke-width="1" stroke-dasharray="4,4" />

    <!-- 9 o'clock Red Lap Triangle Marker -->
    <polygon points="{cx - r + 4},{cy} {cx - r + 20},{cy - 10} {cx - r + 20},{cy + 10}" fill="{ONE_UI_RED}" filter="url(#sw-glow)" />

    <!-- 12 o'clock Subtle Pip -->
    <circle cx="{cx}" cy="{cy - r + 14}" r="4" fill="{ONE_UI_BLUE_LIGHT}" />

    <!-- Hour Hand (Minimal White) -->
    <line x1="{cx}" y1="{cy}" x2="{hx:.1f}" y2="{hy:.1f}" stroke="{ONE_UI_WHITE}" stroke-width="9" stroke-linecap="round" />

    <!-- Minute Hand (One UI Yellow) -->
    <line x1="{cx}" y1="{cy}" x2="{mx:.1f}" y2="{my:.1f}" stroke="{ONE_UI_YELLOW}" stroke-width="7.5" stroke-linecap="round" />

    <!-- Center Pivot Hub -->
    <circle cx="{cx}" cy="{cy}" r="11" fill="{ONE_UI_YELLOW}" stroke="#141416" stroke-width="3.5" />
    <circle cx="{cx}" cy="{cy}" r="3" fill="#141416" />
</svg>"""
    return svg

def generate_android_adaptive_foreground_xml(preset="action_stopwatch"):
    """Generates Android Vector Drawable for adaptive launcher icon foreground."""
    if preset == "minimal_glyph":
        return """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    
    <!-- Outer Dial Track (One UI Blue) -->
    <path
        android:strokeColor="#3E82F7"
        android:strokeWidth="3.2"
        android:fillColor="#0A0A0C"
        android:pathData="M54,26 A28,28 0 1,0 54,82 A28,28 0 1,0 54,26" />
        
    <!-- 12 o'clock Pip (One UI Red) -->
    <path
        android:fillColor="#FF453A"
        android:pathData="M54,24 A3.5,3.5 0 1,0 54,31 A3.5,3.5 0 1,0 54,24" />
        
    <!-- 3, 6, 9 Cardinal Ticks -->
    <path
        android:fillColor="#64B5F6"
        android:pathData="M80.5,53 A1.5,1.5 0 1,0 80.5,56 A1.5,1.5 0 1,0 80.5,53 M54,79.5 A1.5,1.5 0 1,0 54,82.5 A1.5,1.5 0 1,0 54,79.5 M27.5,53 A1.5,1.5 0 1,0 27.5,56 A1.5,1.5 0 1,0 27.5,53" />

    <!-- Hour Hand (White - 10 o'clock) -->
    <path
        android:strokeColor="#FFFFFF"
        android:strokeWidth="4"
        android:strokeLineCap="round"
        android:pathData="M54,54 L42,41" />

    <!-- Minute Hand (One UI Yellow - 2 o'clock) -->
    <path
        android:strokeColor="#FFD60A"
        android:strokeWidth="3.2"
        android:strokeLineCap="round"
        android:pathData="M54,54 L70,43" />

    <!-- Center Pivot Hub -->
    <path
        android:fillColor="#18181B"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:pathData="M54,50 A4,4 0 1,0 54,58 A4,4 0 1,0 54,50" />
        
    <path
        android:fillColor="#FFD60A"
        android:pathData="M54,52.5 A1.5,1.5 0 1,0 54,55.5 A1.5,1.5 0 1,0 54,52.5" />
</vector>
"""
    elif preset == "chrono_ticks":
        return """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    
    <!-- Outer Track -->
    <path
        android:strokeColor="#3E82F7"
        android:strokeWidth="3.5"
        android:fillColor="#0D0D10"
        android:pathData="M54,25 A29,29 0 1,0 54,83 A29,29 0 1,0 54,25" />

    <!-- Major Ticks -->
    <path
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:pathData="M54,26 L54,30 M54,78 L54,82 M82,54 L78,54 M26,54 L30,54" />

    <!-- Hour Hand -->
    <path
        android:strokeColor="#FFFFFF"
        android:strokeWidth="3.8"
        android:strokeLineCap="round"
        android:pathData="M54,54 L41,40" />

    <!-- Minute Hand -->
    <path
        android:strokeColor="#FFD60A"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:pathData="M54,54 L72,42" />

    <!-- Center Pivot -->
    <path
        android:fillColor="#FFD60A"
        android:pathData="M54,51.5 A2.5,2.5 0 1,0 54,56.5 A2.5,2.5 0 1,0 54,51.5" />
</vector>
"""
    else: # action_stopwatch (perfectly centered for adaptive icon)
        return """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    
    <!-- Top Crown Base Stem -->
    <path
        android:fillColor="#64B5F6"
        android:pathData="M52.5,25.5 h3 v2.5 h-3 z" />

    <!-- Top Crown Button -->
    <path
        android:fillColor="#3E82F7"
        android:pathData="M50,22 h8 v4.5 h-8 z" />

    <!-- 45-degree Reset Button -->
    <path
        android:fillColor="#3E82F7"
        android:pathData="M71.5,33.5 L74.5,30.5 L77.5,33.5 L74.5,36.5 Z" />

    <!-- Stopwatch Outer Ring (One UI Blue) -->
    <path
        android:strokeColor="#3E82F7"
        android:strokeWidth="3"
        android:fillColor="#0A0A0E"
        android:pathData="M54,28 A28,28 0 1,0 54,84 A28,28 0 1,0 54,28" />

    <!-- Red Flag Triangle at 9 o'clock -->
    <path
        android:fillColor="#FF453A"
        android:pathData="M29,56 L34,53 L34,59 Z" />

    <!-- 12 o'clock Pip -->
    <path
        android:fillColor="#64B5F6"
        android:pathData="M54,32 A1.2,1.2 0 1,0 54,34.4 A1.2,1.2 0 1,0 54,32" />

    <!-- Hour Hand (White - 10 o'clock) -->
    <path
        android:strokeColor="#FFFFFF"
        android:strokeWidth="3.6"
        android:strokeLineCap="round"
        android:pathData="M54,56 L43,44" />

    <!-- Minute Hand (One UI Yellow - 2 o'clock) -->
    <path
        android:strokeColor="#FFD60A"
        android:strokeWidth="2.8"
        android:strokeLineCap="round"
        android:pathData="M54,56 L69,45" />

    <!-- Center Pivot Hub -->
    <path
        android:fillColor="#FFD60A"
        android:pathData="M54,53.5 A2.5,2.5 0 1,0 54,58.5 A2.5,2.5 0 1,0 54,53.5" />
</vector>
"""

def generate_html_gallery(presets_data):
    """Generates an HTML preview gallery in ~/Downloads/clockity_icons/index.html."""
    cards_html = ""
    for p in presets_data:
        cards_html += f"""
        <div class="card">
            <div class="img-container">
                <img src="{p['png_filename']}" alt="{p['title']}" />
            </div>
            <div class="card-content">
                <h3>{p['title']}</h3>
                <p>{p['desc']}</p>
                <div class="actions">
                    <a href="{p['svg_filename']}" download class="btn">Download SVG</a>
                    <a href="{p['png_filename']}" download class="btn btn-secondary">Download PNG</a>
                </div>
            </div>
        </div>
        """

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Clockity App Icons Gallery</title>
    <style>
        * {{ box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }}
        body {{ background-color: #000000; color: #FFFFFF; padding: 40px 20px; }}
        .header {{ text-align: center; margin-bottom: 40px; }}
        .header h1 {{ font-size: 32px; font-weight: 800; margin-bottom: 8px; color: #FFFFFF; }}
        .header h1 span {{ color: #3E82F7; }}
        .header p {{ color: #8E8E93; font-size: 15px; }}
        .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; max-width: 1100px; margin: 0 auto; }}
        .card {{ background: #1C1C1E; border: 1px solid #2C2C2E; border-radius: 24px; overflow: hidden; display: flex; flex-direction: column; transition: transform 0.2s, border-color 0.2s; }}
        .card:hover {{ transform: translateY(-4px); border-color: #3E82F7; }}
        .img-container {{ background: #0A0A0C; padding: 32px; display: flex; justify-content: center; align-items: center; }}
        .img-container img {{ width: 180px; height: 180px; border-radius: 40px; box-shadow: 0 12px 32px rgba(0,0,0,0.7); }}
        .card-content {{ padding: 20px; flex: 1; display: flex; flex-direction: column; justify-content: space-between; }}
        .card-content h3 {{ font-size: 18px; font-weight: 700; margin-bottom: 6px; color: #FFFFFF; }}
        .card-content p {{ color: #8E8E93; font-size: 13px; line-height: 1.4; margin-bottom: 16px; }}
        .actions {{ display: flex; gap: 8px; }}
        .btn {{ flex: 1; background: #3E82F7; color: #000000; text-align: center; padding: 10px; border-radius: 12px; font-size: 13px; font-weight: 700; text-decoration: none; transition: opacity 0.2s; }}
        .btn:hover {{ opacity: 0.9; }}
        .btn-secondary {{ background: #262629; color: #FFFFFF; }}
        .btn-secondary:hover {{ background: #323236; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>Clockity <span>Icon Studio</span></h1>
        <p>Customizable vector app icons crafted for AMOLED black & One UI aesthetics</p>
    </div>
    <div class="grid">
        {cards_html}
    </div>
</body>
</html>"""
    return html

def apply_icon_to_app(preset_name):
    """Applies the selected icon preset directly into Android app resource directories."""
    print(f"[*] Applying '{preset_name}' icon to Clockity Android App...")
    
    fg_xml = generate_android_adaptive_foreground_xml(preset_name)
    fg_path = os.path.join(RES_DIR, "drawable", "ic_launcher_foreground.xml")
    with open(fg_path, "w") as f:
        f.write(fg_xml)
    print(f"  -> Updated {fg_path}")

    # Generate PNG density icons for legacy launchers
    svg_str = ""
    if preset_name == "minimal_glyph":
        svg_str = generate_minimal_glyph_svg()
    elif preset_name == "chrono_ticks":
        svg_str = generate_chrono_ticks_svg()
    else:
        svg_str = generate_action_stopwatch_svg()

    temp_svg = "/tmp/clockity_temp_apply.svg"
    with open(temp_svg, "w") as f:
        f.write(svg_str)

    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    for folder, size in densities.items():
        folder_path = os.path.join(RES_DIR, folder)
        os.makedirs(folder_path, exist_ok=True)
        out_png = os.path.join(folder_path, "ic_launcher.png")
        out_round = os.path.join(folder_path, "ic_launcher_round.png")
        subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), "-o", out_png, temp_svg], check=True)
        subprocess.run(["cp", out_png, out_round], check=True)
        print(f"  -> Generated {folder}/ic_launcher.png ({size}x{size})")

    print("[✔] Successfully applied icon assets to Android app resources.")

def main():
    parser = argparse.ArgumentParser(description="Clockity SVG Icon Generator")
    parser.add_argument("--all", action="store_true", help="Generate all icon variants, SVGs, and HTML gallery")
    parser.add_argument("--apply", type=str, choices=["minimal_glyph", "chrono_ticks", "action_stopwatch"], help="Apply preset to Android app")
    args = parser.parse_args()

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    presets = [
        {
            "id": "minimal_glyph",
            "title": "Minimalist 3-Color Glyph",
            "desc": "Signature One UI Blue orbit, pure AMOLED black squircle, white/yellow 10:10 hands, and red 12 o'clock alarm pip.",
            "svg_func": generate_minimal_glyph_svg,
            "svg_filename": "clockity_minimal_glyph.svg",
            "png_filename": "clockity_minimal_glyph.png"
        },
        {
            "id": "chrono_ticks",
            "title": "The Chrono Precision Dial",
            "desc": "Detailed 60-second radial tick marks with luminous electric blue outer track and dual precision hands.",
            "svg_func": generate_chrono_ticks_svg,
            "svg_filename": "clockity_chrono_ticks.svg",
            "png_filename": "clockity_chrono_ticks.png"
        },
        {
            "id": "action_stopwatch",
            "title": "Action Stopwatch & Clock",
            "desc": "Tactile stopwatch chassis with top crown button, side pusher, red lap marker, and yellow minute needle.",
            "svg_func": generate_action_stopwatch_svg,
            "svg_filename": "clockity_action_stopwatch.svg",
            "png_filename": "clockity_action_stopwatch.png"
        }
    ]

    print(f"[*] Generating SVGs and PNG previews in {OUTPUT_DIR}...")

    for p in presets:
        svg_content = p["svg_func"](squircle=True)
        svg_path = os.path.join(OUTPUT_DIR, p["svg_filename"])
        png_path = os.path.join(OUTPUT_DIR, p["png_filename"])

        with open(svg_path, "w") as f:
            f.write(svg_content)

        subprocess.run(["rsvg-convert", "-w", "512", "-h", "512", "-o", png_path, svg_path], check=True)
        print(f"  -> Generated {p['svg_filename']} & {p['png_filename']}")

    # Generate Gallery HTML
    html_content = generate_html_gallery(presets)
    html_path = os.path.join(OUTPUT_DIR, "index.html")
    with open(html_path, "w") as f:
        f.write(html_content)
    print(f"[✔] Generated HTML Gallery: {html_path}")

    if args.apply:
        apply_icon_to_app(args.apply)
    elif not args.all:
        # Default to applying minimal_glyph
        apply_icon_to_app("minimal_glyph")

if __name__ == "__main__":
    main()
