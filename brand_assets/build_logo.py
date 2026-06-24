import cairosvg

# ===== ULTRA HD PROFILE PHOTO / LOGO (DP) =====
# Square premium DP: deep lavender->violet radial premium card with the EXACT
# official Enroll+ "Setu" bridge mark centered, plus a subtle wordmark.
LOGO_SVG = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
  <defs>
    <radialGradient id="bg" cx="50%" cy="38%" r="75%">
      <stop offset="0" stop-color="#F2F0FF"/>
      <stop offset="0.55" stop-color="#E6E6FA"/>
      <stop offset="1" stop-color="#D8D5F5"/>
    </radialGradient>
    <linearGradient id="ep-span" x1="320" y1="560" x2="704" y2="560" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#544AB8"/>
      <stop offset="0.5" stop-color="#6C5CE0"/>
      <stop offset="1" stop-color="#544AB8"/>
    </linearGradient>
    <filter id="soft" x="-30%" y="-30%" width="160%" height="160%">
      <feDropShadow dx="0" dy="14" stdDeviation="26" flood-color="#26234D" flood-opacity="0.18"/>
    </filter>
  </defs>

  <!-- full bleed premium plate -->
  <rect width="1024" height="1024" fill="url(#bg)"/>

  <!-- subtle inner ring -->
  <circle cx="512" cy="470" r="300" fill="none" stroke="#6C5CE0" stroke-opacity="0.10" stroke-width="2"/>

  <!-- ===== EXACT official mark geometry, scaled x16, translated to center ===== -->
  <g filter="url(#soft)">
    <!-- span arc: M16 36 Q32 16 48 36  ->  *16 -->
    <path d="M256 576 Q512 256 768 576" stroke="url(#ep-span)" stroke-width="54" stroke-linecap="round" fill="none"/>
    <!-- deck: M14 44 H50 -->
    <path d="M224 704 H800" stroke="#26234D" stroke-width="64" stroke-linecap="round"/>
    <!-- cables -->
    <path d="M352 576 V704" stroke="#26234D" stroke-opacity="0.78" stroke-width="29" stroke-linecap="round"/>
    <path d="M512 416 V704" stroke="#26234D" stroke-opacity="0.78" stroke-width="29" stroke-linecap="round"/>
    <path d="M672 576 V704" stroke="#26234D" stroke-opacity="0.78" stroke-width="29" stroke-linecap="round"/>
    <!-- pillar caps -->
    <circle cx="256" cy="576" r="46" fill="#26234D"/>
    <circle cx="768" cy="576" r="46" fill="#26234D"/>
    <!-- apex keystone node -->
    <circle cx="512" cy="416" r="48" fill="#6C5CE0"/>
  </g>

  <!-- wordmark -->
  <text x="512" y="880" text-anchor="middle"
        font-family="Arial, Helvetica, sans-serif" font-weight="800"
        font-size="120" letter-spacing="-4" fill="#26234D">Enroll<tspan fill="#6C5CE0" dx="-6">+</tspan></text>
</svg>'''

cairosvg.svg2png(bytestring=LOGO_SVG.encode(), write_to="brand_assets/enrollplus-logo-dp.png",
                 output_width=2048, output_height=2048)
print("logo DP -> brand_assets/enrollplus-logo-dp.png (2048x2048)")
