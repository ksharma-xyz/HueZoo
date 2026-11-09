// huezoo-ui.jsx — Web port of Huezoo "Kinetic Vault" primitives.
// Maps 1:1 to existing Compose components in commonMain/ui/components.

// ── Tokens (mirror Color.kt / Dimensions.kt) ─────────────────────────────
const HZ = {
  bg:'#080810', s0:'#0D0D18', s1:'#12121E', s2:'#1C1C2E', s3:'#26263A', s4:'#34343F',
  cyan:'#00E5FF', magenta:'#FF2D78', yellow:'#FFE600',
  purple:'#9B5DE5', green:'#00F5A0', orange:'#FF8A50',
  threshold:'#7B6FF0',
  tx:'#FFFFFF', tx2:'#9898BB', tx3:'#777799',
};

// Near-black foreground for use on bright accents (.onColor in Compose)
const ON_DARK = '#0D0D1A';
function onColor(hex){
  // Purple gets white; everything else gets near-black (matches ColorExt.kt rule)
  if(hex === HZ.purple) return '#FFFFFF';
  return ON_DARK;
}

// ── Typography (HuezooText.kt) ───────────────────────────────────────────
const T = {
  displayLarge:   { fontFamily:'var(--f-display)', fontWeight:700, fontSize:56, lineHeight:1, letterSpacing:'.01em' },
  displayMedium:  { fontFamily:'var(--f-display)', fontWeight:700, fontSize:40, lineHeight:1, letterSpacing:'.01em' },
  displaySmall:   { fontFamily:'var(--f-display)', fontWeight:700, fontSize:28, lineHeight:1, letterSpacing:'.01em' },
  headlineLarge:  { fontFamily:'var(--f-display)', fontWeight:700, fontSize:40, lineHeight:1, letterSpacing:'.02em' },
  headlineMedium: { fontFamily:'var(--f-title)',   fontWeight:600, fontSize:24, lineHeight:1.08 },
  headlineSmall:  { fontFamily:'var(--f-display)', fontWeight:500, fontSize:20, lineHeight:1, letterSpacing:'.03em' },
  titleLarge:     { fontFamily:'var(--f-title)',   fontWeight:700, fontSize:20, lineHeight:1.1, letterSpacing:'.04em' },
  titleMedium:    { fontFamily:'var(--f-title)',   fontWeight:600, fontSize:18, lineHeight:1.15 },
  titleSmall:     { fontFamily:'var(--f-title)',   fontWeight:500, fontSize:15, lineHeight:1.2 },
  bodyLarge:      { fontFamily:'var(--f-body)',    fontWeight:400, fontSize:16, lineHeight:1.4 },
  bodyMedium:     { fontFamily:'var(--f-body)',    fontWeight:400, fontSize:14, lineHeight:1.4 },
  bodySmall:      { fontFamily:'var(--f-body)',    fontWeight:400, fontSize:12, lineHeight:1.4 },
  labelLarge:     { fontFamily:'var(--f-body)',    fontWeight:700, fontSize:14, lineHeight:1, letterSpacing:'.08em', textTransform:'uppercase' },
  labelMedium:    { fontFamily:'var(--f-body)',    fontWeight:500, fontSize:11, lineHeight:1, letterSpacing:'.1em',  textTransform:'uppercase' },
  labelSmall:     { fontFamily:'var(--f-body)',    fontWeight:500, fontSize:10, lineHeight:1, letterSpacing:'.14em', textTransform:'uppercase' },
};

// ── shapedShadow + rimLight (Modifiers.kt) ───────────────────────────────
// Wrap content in a Shelf to get the offset hard-edge shadow + rim light combo.
function Shelf({ children, faceColor=HZ.s2, shelfColor=HZ.s4, dx=4, dy=4, rim=true, rimColor=HZ.cyan, style={}, ...rest }){
  return (
    <div style={{position:'relative', display:'inline-block', ...style}} {...rest}>
      {/* Shelf shadow (drawn behind, offset bottom-right) */}
      <div style={{
        position:'absolute', inset:0, transform:`translate(${dx}px,${dy}px)`,
        background:shelfColor, zIndex:0,
      }}/>
      {/* Face */}
      <div style={{position:'relative', background:faceColor, zIndex:1, height:'100%', width:'100%'}}>
        {children}
        {rim && (
          <div style={{
            position:'absolute', inset:0, pointerEvents:'none',
            boxShadow:`inset 1.5px 1.5px 0 ${hexAlpha(rimColor,.30)}`,
          }}/>
        )}
      </div>
    </div>
  );
}

function hexAlpha(hex,a){
  const h = hex.replace('#','');
  const n = parseInt(h.length===3 ? h.split('').map(c=>c+c).join('') : h, 16);
  const r=(n>>16)&255, g=(n>>8)&255, b=n&255;
  return `rgba(${r},${g},${b},${a})`;
}

// ── HuezooButton.kt ──────────────────────────────────────────────────────
// Pill shape, no rounded corners on Hero cards but buttons themselves are pill (radius:9999).
// Variants from DESIGN_SYSTEM §8.
const BUTTON_VARIANTS = {
  Primary:     { face:HZ.cyan,    shelf:'#0080A0' },
  Confirm:     { face:HZ.green,   shelf:'#008A5A' },
  Danger:      { face:HZ.magenta, shelf:'#8A1A45' },
  Score:       { face:HZ.yellow,  shelf:'#9A8800' },
  Ghost:       { face:'transparent', shelf:'transparent', border:HZ.cyan, textColor:HZ.cyan },
  GhostDanger: { face:'transparent', shelf:'transparent', border:HZ.magenta, textColor:HZ.magenta },
};

function HuezooButton({ variant='Primary', children, onClick, disabled, full=false, style={}, dense=false, ...rest }){
  const v = BUTTON_VARIANTS[variant] || BUTTON_VARIANTS.Primary;
  const fg = v.textColor || onColor(v.face);
  const isGhost = variant.startsWith('Ghost');
  const h = dense ? 44 : 52;
  return (
    <div style={{position:'relative', display: full?'block':'inline-block', width:full?'100%':undefined, ...style}}>
      {!isGhost && (
        <div style={{
          position:'absolute', inset:0, transform:'translate(0,4px)',
          background:v.shelf, borderRadius:9999, zIndex:0,
        }}/>
      )}
      <button
        onClick={disabled?undefined:onClick}
        disabled={disabled}
        style={{
          position:'relative', zIndex:1, width:'100%', height:h,
          background:v.face,
          color:fg, opacity:disabled?.45:1,
          border: isGhost ? `1.5px solid ${v.border}` : 'none',
          borderRadius:9999,
          padding:'0 24px',
          cursor:disabled?'not-allowed':'pointer',
          ...T.labelLarge,
          letterSpacing:'.12em',
          transition:'transform .08s ease',
          WebkitFontSmoothing:'antialiased',
        }}
        onMouseDown={e=>{ if(!disabled) e.currentTarget.style.transform='translateY(2px)'; }}
        onMouseUp={e=>{ e.currentTarget.style.transform='translateY(0)'; }}
        onMouseLeave={e=>{ e.currentTarget.style.transform='translateY(0)'; }}
        {...rest}
      >{children}</button>
    </div>
  );
}

// ── HuezooIconButton ─────────────────────────────────────────────────────
function HuezooIconButton({ variant='Back', children, onClick, style={} }){
  const map = {
    Dismiss: { face:HZ.magenta, shelf:'#8A1A45' },
    Confirm: { face:HZ.green,   shelf:'#008A5A' },
    Back:    { face:HZ.s3,      shelf:HZ.cyan   },
    Info:    { face:HZ.s3,      shelf:HZ.cyan   },
  };
  const v = map[variant] || map.Back;
  return (
    <div style={{position:'relative', display:'inline-block', ...style}}>
      <div style={{position:'absolute', inset:0, transform:'translate(0,4px)', background:v.shelf, borderRadius:14}}/>
      <button onClick={onClick} style={{
        position:'relative', zIndex:1, width:48, height:48, border:'none',
        background:v.face, color:variant==='Dismiss'||variant==='Confirm' ? onColor(v.face) : HZ.tx,
        borderRadius:14, cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center',
      }}>{children}</button>
    </div>
  );
}

// ── SkewedStatChip (parallelogram HUD chip) ──────────────────────────────
function SkewedStatChip({ label, value, color=HZ.cyan, style={} }){
  return (
    <div style={{
      position:'relative', transform:'skewX(-14deg)',
      background:HZ.s2,
      padding:'8px 18px 8px 22px',
      ...style,
    }}>
      <div style={{transform:'skewX(14deg)', display:'flex', flexDirection:'column', gap:2, alignItems:'flex-start'}}>
        <span style={{...T.labelSmall, color:HZ.tx3}}>{label}</span>
        <span style={{...T.displaySmall, color, fontSize:24}}>{value}</span>
      </div>
      {/* left-edge accent */}
      <div style={{position:'absolute', left:0, top:0, bottom:0, width:3, background:color}}/>
    </div>
  );
}

// ── DeltaEBadge ──────────────────────────────────────────────────────────
function deltaEColor(de){
  if (de == null) return HZ.tx2;
  if (de < 1.5) return HZ.magenta;     // hard
  if (de < 3.0) return HZ.yellow;      // medium
  return HZ.cyan;                       // easy
}
function deltaETier(de){
  if (de < 0.5) return 'SUPERHUMAN';
  if (de < 1.0) return 'ELITE';
  if (de < 2.0) return 'EXPERT';
  if (de < 3.0) return 'SHARP';
  if (de < 5.0) return 'TRAINING';
  return 'BEGINNER';
}
function DeltaEBadge({ value, label='Δ E', big=false, align='center' }){
  const c = deltaEColor(value);
  const ai = align==='center' ? 'center' : align==='end' ? 'flex-end' : 'flex-start';
  return (
    <div style={{display:'flex', flexDirection:'column', alignItems:ai, gap:4}}>
      <span style={{...T.labelSmall, color:HZ.tx3}}>{label}</span>
      <span style={{...(big?T.displayLarge:T.displaySmall), color:c, fontVariantNumeric:'tabular-nums'}}>
        {value!=null ? value.toFixed(value<1?2:1) : '—'}
      </span>
    </div>
  );
}

// ── RoundIndicator (dots) ────────────────────────────────────────────────
function RoundIndicator({ total=10, current=0, results=[] }){
  // results: array length=total, entries 'correct' | 'wrong' | undefined
  return (
    <div style={{display:'flex', gap:6, alignItems:'center'}}>
      {Array.from({length:total}).map((_,i)=>{
        const r = results[i];
        const active = i===current && !r;
        let bg = HZ.s3, border='transparent';
        if (r==='correct') bg = HZ.green;
        else if (r==='wrong') bg = HZ.magenta;
        else if (active) { bg = HZ.tx; border = '#fff'; }
        return (
          <div key={i} style={{
            width: active?10:7, height: active?10:7, borderRadius:'50%',
            background:bg,
            outline:active?`2px solid ${hexAlpha(HZ.tx,.4)}`:'none', outlineOffset:1,
            transition:'all .3s cubic-bezier(.34,1.56,.64,1)',
          }}/>
        );
      })}
    </div>
  );
}

// ── HuezooTopBar (HUEZ wordmark version) ─────────────────────────────────
function HuezooTopBar({ left, right, scoreGems=null }){
  return (
    <div style={{
      position:'relative', height:60, padding:'0 16px',
      display:'flex', alignItems:'center', justifyContent:'space-between',
      background:`linear-gradient(180deg, ${hexAlpha(HZ.bg,.95)}, ${hexAlpha(HZ.bg,.82)})`,
      borderBottom:`4px solid ${HZ.s1}`,
      backdropFilter:'blur(12px)',
    }}>
      <div style={{display:'flex', alignItems:'center', gap:12}}>{left}</div>
      <div style={{position:'absolute', left:'50%', top:'50%', transform:'translate(-50%,-50%)'}}>
        <span style={{
          ...T.headlineLarge, fontStyle:'italic', color:HZ.cyan, fontSize:30,
          textShadow:`0 0 22px ${hexAlpha(HZ.cyan,.5)}`,
        }}>HUEZ</span>
      </div>
      <div style={{display:'flex', alignItems:'center', gap:12}}>{right}</div>
    </div>
  );
}

// ── AmbientGlowBackground ────────────────────────────────────────────────
function AmbientGlow({ primary=HZ.cyan, secondary=HZ.magenta, children, style={} }){
  return (
    <div style={{
      position:'relative', width:'100%', height:'100%',
      background:`
        radial-gradient(circle at 0% 0%, ${hexAlpha(primary,.10)}, transparent 55%),
        radial-gradient(circle at 100% 100%, ${hexAlpha(secondary,.10)}, transparent 55%),
        ${HZ.bg}
      `,
      ...style,
    }}>{children}</div>
  );
}

// ── Color science: ΔE → produce two colors with that perceptual difference ─
// Simplified: we approximate ΔE in Lab L* space — ΔE 1 ≈ 1 unit of L difference,
// which is roughly correct for CIEDE2000 on chromatic colors. Real game uses
// pure-Kotlin CIEDE2000 in commonMain/domain/color/ColorMath.kt.
function rgbToLab(r,g,b){
  r/=255; g/=255; b/=255;
  [r,g,b]=[r,g,b].map(c=>c>.04045?Math.pow((c+.055)/1.055,2.4):c/12.92);
  // sRGB -> XYZ (D65)
  const x = r*.4124+g*.3576+b*.1805;
  const y = r*.2126+g*.7152+b*.0722;
  const z = r*.0193+g*.1192+b*.9505;
  const Xn=.95047, Yn=1, Zn=1.08883;
  const f=t=>t>0.008856?Math.cbrt(t):(7.787*t+16/116);
  const L=116*f(y/Yn)-16, a=500*(f(x/Xn)-f(y/Yn)), bb=200*(f(y/Yn)-f(z/Zn));
  return [L,a,bb];
}
function labToRgb(L,a,b){
  const Xn=.95047, Yn=1, Zn=1.08883;
  const fy=(L+16)/116, fx=a/500+fy, fz=fy-b/200;
  const f3=t=>{const t3=t*t*t; return t3>0.008856?t3:(t-16/116)/7.787;};
  const X=Xn*f3(fx), Y=Yn*f3(fy), Z=Zn*f3(fz);
  let r= X*3.2406 + Y*-1.5372 + Z*-0.4986;
  let g= X*-0.9689 + Y*1.8758 + Z*0.0415;
  let bl=X*0.0557 + Y*-0.2040 + Z*1.0570;
  [r,g,bl]=[r,g,bl].map(c=>c>0.0031308?1.055*Math.pow(c,1/2.4)-0.055:12.92*c);
  return [r,g,bl].map(c=>Math.max(0,Math.min(255,Math.round(c*255))));
}
function rgbStr([r,g,b]){return `rgb(${r},${g},${b})`}

// Generate a vivid base color (matches ColorEngine.randomVividColor — uses hue rotation)
function randomVividColor(seed){
  // Use a deterministic-ish PRNG so the prototype is stable
  const rand = seed!=null ? mulberry32(seed) : Math.random;
  // Pick HSL with high saturation, mid lightness
  const h = rand()*360, s = 70+rand()*20, l = 50+rand()*10;
  return hslToRgb(h,s,l);
}
function mulberry32(a){return function(){let t=a+=0x6D2B79F5;t=Math.imul(t^t>>>15,t|1);t^=t+Math.imul(t^t>>>7,t|61);return((t^t>>>14)>>>0)/4294967296;};}
function hslToRgb(h,s,l){
  s/=100; l/=100;
  const k=n=>(n+h/30)%12, a=s*Math.min(l,1-l);
  const f=n=>l-a*Math.max(-1,Math.min(k(n)-3,Math.min(9-k(n),1)));
  return [Math.round(f(0)*255),Math.round(f(8)*255),Math.round(f(4)*255)];
}

// Given a base RGB color and target ΔE, produce an offset color whose Lab distance
// from base is approximately ΔE. Perturb mostly in L* (and lightly in a,b) for
// best perceptual variety. ΔE math is approximate — the real game uses CIEDE2000.
function offsetByDeltaE(rgb, deltaE, sign=1){
  const [L,a,b] = rgbToLab(...rgb);
  // Distribute the delta across L (60%) and a/b (40%) to feel natural
  const dL = deltaE*0.6*sign;
  // Random direction in a/b plane
  const theta = Math.random()*Math.PI*2;
  const dRad = deltaE*0.8*sign;
  const da = Math.cos(theta)*dRad;
  const db = Math.sin(theta)*dRad;
  // Keep L in display range
  const newL = Math.max(20, Math.min(85, L+dL));
  return labToRgb(newL, a+da, b+db);
}

// Generate the colour pair for round n. If isSame, both colors equal.
function generateRoundColors({ round, deltaE, isSame, seed }){
  const base = randomVividColor(seed + round*7919);
  if (isSame) return { a: rgbStr(base), b: rgbStr(base), actualDeltaE: 0 };
  const sign = (seed+round)%2===0 ? 1 : -1;
  const offset = offsetByDeltaE(base, deltaE, sign);
  return { a: rgbStr(base), b: rgbStr(offset), actualDeltaE: deltaE };
}

// ΔE curve for Color Memory Match (10 rounds, tightens each round).
function dEForRound(round, difficultyScale=1){
  // Round 1..10, e starts 5.0, ends near 0.5
  const curve = [5.0, 4.0, 3.0, 2.5, 2.0, 1.5, 1.2, 1.0, 0.7, 0.5];
  const v = curve[Math.min(round-1, curve.length-1)] ?? 0.5;
  return Math.max(0.2, v*difficultyScale);
}

// Score helpers
function scoreDelta(correct){ return correct ? 10 : -5; }
function totalToTier(score){
  if (score >= 90) return 'PERFECT EYE';
  if (score >= 70) return 'STRONG RECALL';
  if (score >= 40) return 'SOLID';
  if (score >= 10) return 'ROOM TO GROW';
  return 'EYES DRIFTED';
}

// ── Sting copy pool ──────────────────────────────────────────────────────
const STING_CORRECT = {
  easy:   ['Locked in.', 'Eyes still warm.', 'Steady.'],
  mid:    ['Sharp.', 'Eye remembered.', 'Held the line.'],
  hard:   ['Real recall.', 'That one was real.', 'Eyes did not blink.'],
  elite:  ['Beyond impressive.', 'Your retina is a recorder.', 'Eyes elite.'],
};
const STING_WRONG = {
  easy:   ['That gap was wide. Hmm.', 'Warmer than that.', 'The eye wandered.'],
  mid:    ['Close, but ΔE {de} got you.', 'Memory blinked at ΔE {de}.', 'Almost. ΔE {de}.'],
  hard:   ['ΔE {de}. Sub-pixel territory.', 'You were right there. ΔE {de}.', 'Memory faded at ΔE {de}.'],
  elite:  ['ΔE {de}. Barely real. Still missed.', 'Below human limits. So is the gap.', 'You almost out-saw your own eye.'],
};
function pickSting(pool, de){
  let bucket = 'easy';
  if (de < 1.0) bucket = 'elite';
  else if (de < 2.0) bucket = 'hard';
  else if (de < 3.5) bucket = 'mid';
  const arr = pool[bucket];
  const line = arr[Math.floor(Math.random()*arr.length)];
  return line.replace('{de}', de.toFixed(de<1?2:1));
}

// Final result copy by score
function resultStingByScore(score){
  if (score >= 95) return 'Superhuman recall. Seriously.';
  if (score >= 80) return 'Your memory is elite.';
  if (score >= 60) return 'Sharp. Very sharp.';
  if (score >= 40) return 'Better than most.';
  if (score >= 20) return 'Room to grow.';
  if (score > 0)   return 'Keep training.';
  return 'The eye drifted today. Come back.';
}

// ── Confetti ─────────────────────────────────────────────────────────────
function Confetti({ active, color=HZ.cyan, count=22 }){
  if (!active) return null;
  const parts = Array.from({length:count}).map((_,i)=>{
    const angle = (Math.PI*2)*(i/count);
    const dist = 80+Math.random()*120;
    const cx = Math.cos(angle)*dist, cy = Math.sin(angle)*dist-30;
    const cr = (Math.random()-0.5)*720;
    const colors=[color, HZ.cyan, HZ.yellow, HZ.tx];
    return (
      <div key={i} style={{
        position:'absolute', left:'50%', top:'50%', width:8, height:14,
        background:colors[i%colors.length],
        animation:'hz-confetti 1.2s cubic-bezier(.16,1,.3,1) forwards',
        ['--cx']:`${cx}px`, ['--cy']:`${cy}px`, ['--cr']:`${cr}deg`,
        pointerEvents:'none',
      }}/>
    );
  });
  return <div style={{position:'absolute', inset:0, pointerEvents:'none', zIndex:60}}>{parts}</div>;
}

Object.assign(window, {
  HZ, T, onColor, hexAlpha, Shelf,
  HuezooButton, HuezooIconButton, SkewedStatChip,
  DeltaEBadge, deltaEColor, deltaETier, RoundIndicator, HuezooTopBar,
  AmbientGlow, Confetti,
  generateRoundColors, dEForRound, scoreDelta, totalToTier,
  pickSting, STING_CORRECT, STING_WRONG, resultStingByScore, rgbStr,
});
