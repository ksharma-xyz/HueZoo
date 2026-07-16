// color-memory-match.jsx — Main app + two variation flows.
//
// Game (per VISION.md game 6):
//   • Color A shown for ~3s, then hidden
//   • Color B shown → SAME or DIFFERENT?
//   • Correct +10, wrong −5
//   • 10 rounds, ΔE tightens each round
//
// Engineering map (handoff):
//   App                         → CMMatchScreen.kt   (new screen)
//   Phase state machine         → CMMatchViewModel.kt + state/CMMatchUiState.kt
//   useRoundColors()            → ColorEngine.generateRoundColors() + dEForRound()
//   VariationVault / TwinLock   → ui/games/colormemory/{VaultLayout,TwinLockLayout}.kt
//   HUD                         → SkewedStatChip + RoundIndicator (reused)
//   Result screen               → ResultCard.kt (reused) + new CMMatchResultScreen.kt

const PHASES = {
  Ready:    'ready',     // round intro
  Memory:   'memory',    // Color A shown for ~3s, segmented ring counts down
  Hold:     'hold',      // brief blank "REMEMBER" interstitial
  Recall:   'recall',    // Color B shown, SAME/DIFFERENT CTAs
  Feedback: 'feedback',  // reveal correct + sting copy
  Result:   'result',    // end of session
};
const TOTAL_ROUNDS = 10;
const MEMORY_MS = 3000;
const HOLD_MS = 400;
const FEEDBACK_MS = 1700;

// ─────────────────────────────────────────────────────────────────────────
// Game state hook
// ─────────────────────────────────────────────────────────────────────────
function useGame({ difficultyScale, paused, forcedAnswer, seed }){
  const [round, setRound] = React.useState(1);          // 1..10
  const [phase, setPhase] = React.useState(PHASES.Memory);
  const [score, setScore] = React.useState(0);
  const [results, setResults] = React.useState([]);     // per-round 'correct'|'wrong'
  // Lazy-init colors so the very first paint has a swatch (not null).
  const initial = React.useMemo(()=>{
    const same = forcedAnswer === 'same' ? true
              : forcedAnswer === 'diff' ? false
              : Math.random() < 0.5;
    const de = dEForRound(1, difficultyScale);
    return {
      colors: { ...generateRoundColors({ round:1, deltaE: de, isSame: same, seed: seed + 101 }), deltaE: de },
      trueIsSame: same,
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  const [colors, setColors] = React.useState(initial.colors);
  const [lastAnswer, setLastAnswer] = React.useState(null); // {correct,playerSaid,truth,de}
  const [trueIsSame, setTrueIsSame] = React.useState(initial.trueIsSame);
  const timers = React.useRef([]);

  const clearTimers = () => { timers.current.forEach(clearTimeout); timers.current = []; };
  const after = (ms, fn) => { const id = setTimeout(fn, ms); timers.current.push(id); };

  // Build colors when round/phase enters Memory
  React.useEffect(()=>{
    if (phase !== PHASES.Memory) return;
    const same = forcedAnswer === 'same' ? true
              : forcedAnswer === 'diff' ? false
              : Math.random() < 0.5;
    setTrueIsSame(same);
    const de = dEForRound(round, difficultyScale);
    const c = generateRoundColors({ round, deltaE: de, isSame: same, seed: seed + round*101 });
    setColors({ ...c, deltaE: de });
    if (paused) return;
    after(MEMORY_MS, ()=> setPhase(PHASES.Hold));
    return clearTimers;
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, round, seed]);

  React.useEffect(()=>{
    if (phase !== PHASES.Hold || paused) return;
    after(HOLD_MS, ()=> setPhase(PHASES.Recall));
    return clearTimers;
  }, [phase, paused]);

  // Apply pause (stop all pending timers)
  React.useEffect(()=>{
    if (paused) clearTimers();
  }, [paused]);

  function answer(playerSaidSame){
    if (phase !== PHASES.Recall) return;
    const correct = playerSaidSame === trueIsSame;
    setLastAnswer({ correct, playerSaid: playerSaidSame, truth: trueIsSame, de: colors.deltaE });
    setResults(prev => { const next=[...prev]; next[round-1] = correct?'correct':'wrong'; return next; });
    setScore(s => s + scoreDelta(correct));
    setPhase(PHASES.Feedback);
    after(FEEDBACK_MS, ()=>{
      if (round >= TOTAL_ROUNDS) setPhase(PHASES.Result);
      else { setRound(r=>r+1); setPhase(PHASES.Memory); }
    });
  }

  function reset(){
    clearTimers();
    setRound(1); setScore(0); setResults([]); setColors(null); setLastAnswer(null);
    setPhase(PHASES.Memory);
  }

  function jumpTo(targetPhase, targetRound=round){
    clearTimers();
    setRound(targetRound); setPhase(targetPhase);
  }

  // For force-state preview: synthesize a mock finished session.
  function mockResult(){
    clearTimers();
    const fake = ['correct','correct','wrong','correct','correct','correct','wrong','correct','correct','correct'];
    setResults(fake);
    setScore(fake.filter(r=>r==='correct').length * 10 + fake.filter(r=>r==='wrong').length * -5);
    setRound(TOTAL_ROUNDS);
    setPhase(PHASES.Result);
  }

  return {
    round, phase, score, results, colors, lastAnswer, trueIsSame,
    answer, reset, jumpTo, mockResult,
  };
}

// ─────────────────────────────────────────────────────────────────────────
// Segmented countdown ring (cine_solid aesthetic — from VISION.md)
// Used by Variation A around the central swatch during MEMORY phase.
// ─────────────────────────────────────────────────────────────────────────
function CountdownRing({ size=260, segments=24, durationMs=3000, color=HZ.cyan, active=true }){
  const [t, setT] = React.useState(0);
  const start = React.useRef(null);
  React.useEffect(()=>{
    if (!active) { setT(0); return; }
    start.current = performance.now();
    let raf;
    const tick = (now)=>{
      const elapsed = now - start.current;
      const p = Math.min(1, elapsed/durationMs);
      setT(p);
      if (p<1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return ()=> cancelAnimationFrame(raf);
  }, [active, durationMs]);

  const cx = size/2, cy = size/2;
  const r = size/2 - 14;
  const innerR = r - 12;
  const segs = [];
  const gap = 3; // deg
  for (let i=0;i<segments;i++){
    const segAngle = 360/segments;
    const a0 = (-90 + i*segAngle) * Math.PI/180;
    const a1 = (-90 + (i+1)*segAngle - gap) * Math.PI/180;
    const x0o = cx+Math.cos(a0)*r,   y0o = cy+Math.sin(a0)*r;
    const x1o = cx+Math.cos(a1)*r,   y1o = cy+Math.sin(a1)*r;
    const x1i = cx+Math.cos(a1)*innerR, y1i = cy+Math.sin(a1)*innerR;
    const x0i = cx+Math.cos(a0)*innerR, y0i = cy+Math.sin(a0)*innerR;
    const path = `M${x0o},${y0o} A${r},${r} 0 0 1 ${x1o},${y1o} L${x1i},${y1i} A${innerR},${innerR} 0 0 0 ${x0i},${y0i} Z`;
    const drained = (i/segments) < t;
    segs.push(
      <path key={i} d={path}
        fill={drained ? hexAlpha(color,.08) : color}
        opacity={drained?1:.95}
      />
    );
  }
  // Center cross + tick marks for tactical feel
  return (
    <svg width={size} height={size} style={{position:'absolute', inset:0}}>
      {segs}
      {/* corner brackets */}
      {[[8,8],[size-8,8],[8,size-8],[size-8,size-8]].map(([x,y],i)=>{
        const sx = x<size/2?1:-1, sy = y<size/2?1:-1;
        return (
          <g key={i} stroke={hexAlpha(color,.5)} strokeWidth="1.5" fill="none">
            <line x1={x} y1={y} x2={x+sx*10} y2={y}/>
            <line x1={x} y1={y} x2={x} y2={y+sy*10}/>
          </g>
        );
      })}
    </svg>
  );
}

// ─────────────────────────────────────────────────────────────────────────
// Shared HUD: round + score chips + dots
// ─────────────────────────────────────────────────────────────────────────
function GameHUD({ round, score, results }){
  return (
    <div style={{padding:'14px 20px 8px', display:'flex', flexDirection:'column', gap:14}}>
      <div style={{display:'flex', gap:12, alignItems:'center'}}>
        <SkewedStatChip label="ROUND" value={`${String(round).padStart(2,'0')}/${TOTAL_ROUNDS}`} color={HZ.cyan} style={{paddingLeft:'24px'}}/>
        <SkewedStatChip label="SCORE" value={score} color={HZ.yellow} style={{paddingLeft:'24px'}}/>
      </div>
      <RoundIndicator total={TOTAL_ROUNDS} current={round-1} results={results}/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────
// Variation A — "The Vault"
//   Single big central swatch. Memory phase shows a segmented countdown ring.
//   Recall phase replaces the swatch with Color B; SAME/DIFFERENT buttons below.
//   Feedback shows the two side-by-side at small scale.
// ─────────────────────────────────────────────────────────────────────────
function VariationVault({ game }){
  const { phase, round, score, results, colors, lastAnswer, answer } = game;
  const isFeedback = phase === PHASES.Feedback;
  const wrong = isFeedback && !lastAnswer?.correct;

  return (
    <>
      <GameHUD round={round} score={score} results={results}/>

      {/* Headline copy slot */}
      <div style={{padding:'2px 24px 6px', minHeight:48, display:'flex', flexDirection:'column', justifyContent:'flex-end'}}>
        {phase === PHASES.Memory && (
          <>
            <div style={{...T.labelSmall, color:HZ.cyan, ...{transition:'opacity .2s'}}}>◉ MEMORY PHASE</div>
            <div style={{...T.titleLarge, color:HZ.tx, marginTop:4}}>Hold this color.</div>
          </>
        )}
        {phase === PHASES.Hold && (
          <div style={{...T.titleLarge, color:HZ.tx, marginTop:4}} className="hz-pulse">Remember…</div>
        )}
        {phase === PHASES.Recall && (
          <>
            <div style={{...T.labelSmall, color:HZ.magenta}}>◉ RECALL</div>
            <div style={{...T.titleLarge, color:HZ.tx, marginTop:4}}>Same color — or different?</div>
          </>
        )}
        {isFeedback && (
          <>
            <div style={{...T.labelSmall, color: wrong?HZ.magenta:HZ.green}}>
              {wrong ? '◉ EYE DRIFTED' : '◉ LOCKED IN'}
            </div>
            <div style={{...T.titleLarge, color:HZ.tx, marginTop:4}}>
              {wrong
                ? pickSting(STING_WRONG, lastAnswer.de)
                : pickSting(STING_CORRECT, lastAnswer.de)}
            </div>
          </>
        )}
      </div>

      {/* Central swatch area */}
      <div style={{
        position:'relative', display:'flex', alignItems:'center', justifyContent:'center',
        padding:'12px 0', minHeight:300,
      }}>
        {/* Ring lives in memory phase only */}
        <div style={{position:'relative', width:280, height:280}}>
          {phase === PHASES.Memory && (
            <CountdownRing size={280} segments={24} durationMs={MEMORY_MS} color={HZ.cyan} active={true}/>
          )}

          {/* The swatch */}
          {phase === PHASES.Memory && (
            <SwatchSquare color={colors?.a} size={220} accent={HZ.cyan}/>
          )}
          {phase === PHASES.Hold && (
            <SealedChamber size={220}/>
          )}
          {phase === PHASES.Recall && (
            <SwatchSquare color={colors?.b} size={220} accent={HZ.magenta} keyId="recall"/>
          )}
          {isFeedback && (
            <FeedbackCompare colorA={colors?.a} colorB={colors?.b} truthSame={game.trueIsSame} playerSaidSame={lastAnswer?.playerSaid} wrong={wrong}/>
          )}
        </div>
      </div>

      {/* ΔE meta + action area */}
      <div style={{padding:'4px 24px 8px', display:'flex', justifyContent:'space-between', alignItems:'flex-end'}}>
        <div>
          <div style={{...T.labelSmall, color:HZ.tx3}}>THIS ROUND</div>
          <div style={{display:'flex', gap:14, alignItems:'baseline'}}>
            <span style={{...T.displaySmall, color: deltaEColor(colors?.deltaE), fontVariantNumeric:'tabular-nums'}}>
              ΔE {colors?.deltaE?.toFixed(colors?.deltaE<1?2:1)}
            </span>
            <span style={{...T.labelSmall, color:HZ.tx3}}>{deltaETier(colors?.deltaE ?? 5)}</span>
          </div>
        </div>
        {isFeedback && (
          <span style={{...T.labelSmall, color: wrong?HZ.magenta:HZ.green}}>
            {wrong ? '−5 PTS' : '+10 PTS'}
          </span>
        )}
      </div>

      {/* Buttons */}
      <div style={{padding:'14px 20px 24px', display:'flex', gap:12}}>
        <HuezooButton variant={phase===PHASES.Recall?'Primary':'Ghost'} full disabled={phase!==PHASES.Recall}
          onClick={()=>answer(true)}>
          SAME
        </HuezooButton>
        <HuezooButton variant={phase===PHASES.Recall?'Danger':'GhostDanger'} full disabled={phase!==PHASES.Recall}
          onClick={()=>answer(false)}>
          DIFFERENT
        </HuezooButton>
      </div>
    </>
  );
}

function SwatchSquare({ color, size=220, accent=HZ.cyan, keyId='' }){
  return (
    <div key={keyId} style={{
      position:'absolute', left:'50%', top:'50%', transform:'translate(-50%,-50%)',
      width:size, height:size,
    }}>
      <div style={{position:'relative', width:'100%', height:'100%'}}>
        {/* shelf */}
        <div style={{position:'absolute', inset:0, transform:'translate(6px,6px)', background:HZ.s4, zIndex:0}}/>
        <div style={{
          position:'relative', zIndex:1, width:'100%', height:'100%',
          background:color || HZ.s2,
          boxShadow:`inset 2px 2px 0 ${hexAlpha('#FFFFFF',.18)}, inset -2px -2px 0 ${hexAlpha('#000000',.18)}, 0 0 0 1.5px ${hexAlpha(accent,.4)}`,
          transition:'background .35s ease',
        }}>
          {/* corner ticks */}
          {[[0,0,1,1],[size-12,0,-1,1],[0,size-12,1,-1],[size-12,size-12,-1,-1]].map(([x,y,sx,sy],i)=>(
            <svg key={i} width="14" height="14" style={{position:'absolute', left:x, top:y}}>
              <path d={`M${sx<0?14:0},${sy<0?14:0} L${sx<0?6:8},${sy<0?14:0}`} stroke={hexAlpha('#fff',.5)} strokeWidth="1.5"/>
              <path d={`M${sx<0?14:0},${sy<0?14:0} L${sx<0?14:0},${sy<0?6:8}`} stroke={hexAlpha('#fff',.5)} strokeWidth="1.5"/>
            </svg>
          ))}
        </div>
      </div>
    </div>
  );
}

function SealedChamber({ size=220 }){
  return (
    <div style={{
      position:'absolute', left:'50%', top:'50%', transform:'translate(-50%,-50%)',
      width:size, height:size,
    }}>
      <div style={{position:'absolute', inset:0, transform:'translate(6px,6px)', background:HZ.s4, zIndex:0}}/>
      <div style={{
        position:'relative', zIndex:1, width:'100%', height:'100%',
        background:HZ.s1,
        boxShadow:`inset 1.5px 1.5px 0 ${hexAlpha(HZ.cyan,.30)}`,
        display:'flex', alignItems:'center', justifyContent:'center', overflow:'hidden',
      }}>
        {/* stripes */}
        <svg width={size} height={size} style={{position:'absolute', inset:0, opacity:.18}}>
          {Array.from({length:18}).map((_,i)=>(
            <line key={i} x1={-50+i*22} y1={size+10} x2={size+10} y2={-50+i*22}
              stroke={HZ.cyan} strokeWidth="1.5"/>
          ))}
        </svg>
        <div style={{textAlign:'center'}}>
          <div style={{...T.labelSmall, color:HZ.cyan, marginBottom:8}} className="hz-blink">◉ SEALED</div>
          <div style={{...T.headlineSmall, color:HZ.tx}}>HOLD</div>
        </div>
      </div>
    </div>
  );
}

function FeedbackCompare({ colorA, colorB, truthSame, playerSaidSame, wrong }){
  // Show A and B side-by-side at smaller scale; outline correct in green.
  const sz = 156;
  const truth = truthSame ? 'SAME' : 'DIFFERENT';
  return (
    <div style={{
      position:'absolute', left:'50%', top:'50%', transform:'translate(-50%,-50%)',
      width:'100%', height:'100%',
    }}>
      <div style={{
        width:'100%', height:'100%',
        display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', gap:10,
      }}>
        <div style={{display:'flex', gap:18, alignItems:'center'}}>
          <ChamberMini color={colorA} label="A" size={sz}/>
          <div style={{...T.headlineLarge, color: wrong?HZ.magenta:HZ.green, fontSize:36}}>
            {truthSame ? '=' : '≠'}
          </div>
          <ChamberMini color={colorB} label="B" size={sz}/>
        </div>
        <div style={{...T.labelSmall, color:HZ.tx3, letterSpacing:'.18em'}}>
          TRUTH: <span style={{color: wrong?HZ.magenta:HZ.green}}>{truth}</span>
        </div>
      </div>
    </div>
  );
}

function ChamberMini({ color, label, size=120 }){
  return (
    <div style={{display:'flex', flexDirection:'column', alignItems:'center', gap:6}}>
      <div style={{position:'relative', width:size, height:size}}>
        <div style={{position:'absolute', inset:0, transform:'translate(4px,4px)', background:HZ.s4}}/>
        <div style={{
          position:'relative', width:'100%', height:'100%', background:color || HZ.s2,
          boxShadow:`inset 1.5px 1.5px 0 ${hexAlpha('#FFFFFF',.15)}`,
        }}/>
      </div>
      <div style={{...T.labelSmall, color:HZ.tx3}}>{label}</div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────
// Variation B — "Twin Lock"
//   Two adjacent vertical chambers. Left chamber shows Color A, then a shutter
//   drops over it ("SEALED"). Right chamber reveals Color B with a "?" preroll.
//   On feedback: left chamber's shutter slides back up to reveal the original.
// ─────────────────────────────────────────────────────────────────────────
function VariationTwinLock({ game }){
  const { phase, round, score, results, colors, lastAnswer, answer, trueIsSame } = game;
  const isFeedback = phase === PHASES.Feedback;
  const wrong = isFeedback && !lastAnswer?.correct;

  return (
    <>
      <GameHUD round={round} score={score} results={results}/>

      {/* Headline */}
      <div style={{padding:'2px 24px 6px', minHeight:48}}>
        {phase === PHASES.Memory && (
          <>
            <div style={{...T.labelSmall, color:HZ.cyan}}>◉ CHAMBER A LIVE</div>
            <div style={{...T.titleLarge, color:HZ.tx, marginTop:4}}>Hold this color.</div>
          </>
        )}
        {phase === PHASES.Hold && (
          <>
            <div style={{...T.labelSmall, color:HZ.yellow}}>◉ CHAMBER A SEALED</div>
            <div style={{...T.titleLarge, color:HZ.tx, marginTop:4}}>Stand by…</div>
          </>
        )}
        {phase === PHASES.Recall && (
          <>
            <div style={{...T.labelSmall, color:HZ.magenta}}>◉ CHAMBER B LIVE — RECALL</div>
            <div style={{...T.titleLarge, color:HZ.tx, marginTop:4}}>Match the seal?</div>
          </>
        )}
        {isFeedback && (
          <>
            <div style={{...T.labelSmall, color: wrong?HZ.magenta:HZ.green}}>
              {wrong ? '◉ MEMORY DRIFTED' : '◉ MEMORY VERIFIED'}
            </div>
            <div style={{...T.titleLarge, color:HZ.tx, marginTop:4}}>
              {wrong
                ? pickSting(STING_WRONG, lastAnswer.de)
                : pickSting(STING_CORRECT, lastAnswer.de)}
            </div>
          </>
        )}
      </div>

      {/* Twin chambers */}
      <div style={{
        padding:'12px 20px', display:'flex', gap:10, alignItems:'stretch', minHeight:320,
      }}>
        <Chamber
          label="A"
          color={colors?.a}
          accent={HZ.cyan}
          state={
            phase===PHASES.Memory ? 'live'
            : (phase===PHASES.Hold || phase===PHASES.Recall) ? 'sealed'
            : 'revealed'
          }
        />
        <Chamber
          label="B"
          color={colors?.b}
          accent={HZ.magenta}
          state={
            phase===PHASES.Memory ? 'waiting'
            : phase===PHASES.Hold ? 'waiting'
            : (phase===PHASES.Recall) ? 'live'
            : 'revealed'
          }
        />
      </div>

      {/* ΔE row */}
      <div style={{padding:'4px 24px 8px', display:'flex', justifyContent:'space-between', alignItems:'flex-end'}}>
        <div>
          <div style={{...T.labelSmall, color:HZ.tx3}}>THIS ROUND</div>
          <div style={{display:'flex', gap:14, alignItems:'baseline'}}>
            <span style={{...T.displaySmall, color: deltaEColor(colors?.deltaE), fontVariantNumeric:'tabular-nums'}}>
              ΔE {colors?.deltaE?.toFixed(colors?.deltaE<1?2:1)}
            </span>
            <span style={{...T.labelSmall, color:HZ.tx3}}>{deltaETier(colors?.deltaE ?? 5)}</span>
          </div>
        </div>
        {isFeedback && (
          <span style={{...T.labelSmall, color: wrong?HZ.magenta:HZ.green}}>
            {wrong ? '−5 PTS' : '+10 PTS'}
          </span>
        )}
      </div>

      {/* Buttons */}
      <div style={{padding:'14px 20px 24px', display:'flex', gap:12}}>
        <HuezooButton variant={phase===PHASES.Recall?'Primary':'Ghost'} full disabled={phase!==PHASES.Recall}
          onClick={()=>answer(true)}>SAME</HuezooButton>
        <HuezooButton variant={phase===PHASES.Recall?'Danger':'GhostDanger'} full disabled={phase!==PHASES.Recall}
          onClick={()=>answer(false)}>DIFFERENT</HuezooButton>
      </div>
    </>
  );
}

// Chamber states: live (showing color), sealed (shutter down), revealed (final reveal),
// waiting (locked, awaiting turn)
function Chamber({ label, color, accent=HZ.cyan, state='waiting' }){
  const showColor = state === 'live' || state === 'revealed';
  const showShutter = state === 'sealed';
  return (
    <div style={{position:'relative', flex:1, minWidth:0}}>
      {/* shelf */}
      <div style={{position:'absolute', inset:0, transform:'translate(4px,4px)', background:HZ.s4, zIndex:0}}/>
      <div style={{
        position:'relative', zIndex:1, width:'100%', height:'100%',
        background: showColor ? color : HZ.s1,
        boxShadow:`inset 2px 2px 0 ${hexAlpha('#FFFFFF', showColor?.16:0)}, inset 1.5px 1.5px 0 ${hexAlpha(accent,.30)}`,
        overflow:'hidden', display:'flex', flexDirection:'column',
        transition:'background .25s ease',
      }}>
        {/* top tag */}
        <div style={{
          position:'absolute', top:8, left:8, right:8, zIndex:5,
          display:'flex', justifyContent:'space-between', alignItems:'center',
        }}>
          <span style={{...T.labelSmall, color: showColor?'#fff':accent, mixBlendMode: showColor?'difference':'normal'}}>
            CHAMBER {label}
          </span>
          <Indicator state={state} accent={accent}/>
        </div>

        {/* waiting state: stripes + ? */}
        {state === 'waiting' && (
          <div style={{flex:1, position:'relative', display:'flex', alignItems:'center', justifyContent:'center', overflow:'hidden'}}>
            <svg width="100%" height="100%" style={{position:'absolute', inset:0, opacity:.12}}>
              {Array.from({length:12}).map((_,i)=>(
                <line key={i} x1={-30+i*30} y1={300} x2={300} y2={-30+i*30}
                  stroke={accent} strokeWidth="1.2"/>
              ))}
            </svg>
            <div style={{...T.displayLarge, color:hexAlpha(accent,.55), fontSize:96}}>?</div>
          </div>
        )}

        {/* shutter overlay */}
        {showShutter && (
          <div className="hz-shutter-close" style={{
            position:'absolute', inset:0, zIndex:4,
            background:`repeating-linear-gradient(180deg, ${HZ.s2} 0 6px, ${HZ.s1} 6px 12px)`,
            display:'flex', alignItems:'center', justifyContent:'center',
            boxShadow:`inset 0 0 0 1.5px ${hexAlpha(HZ.yellow,.35)}`,
          }}>
            <div style={{textAlign:'center'}}>
              <div style={{...T.labelSmall, color:HZ.yellow, marginBottom:6}} className="hz-blink">◉ SEALED</div>
              <div style={{...T.headlineSmall, color:HZ.tx}}>HOLD</div>
            </div>
          </div>
        )}

        {/* revealed: subtle "ORIGINAL" tag on chamber A */}
        {state === 'revealed' && label==='A' && (
          <div style={{position:'absolute', bottom:10, left:0, right:0, textAlign:'center', zIndex:5}}>
            <span style={{
              ...T.labelSmall, color:'#fff', mixBlendMode:'difference',
              padding:'4px 8px', background:hexAlpha('#000',.3),
            }}>UNSEALED</span>
          </div>
        )}
      </div>
    </div>
  );
}

function Indicator({ state, accent }){
  const map = {
    live:     { c: accent,    text:'LIVE'   },
    sealed:   { c: HZ.yellow, text:'SEAL'   },
    waiting:  { c: HZ.tx3,    text:'WAIT'   },
    revealed: { c: HZ.green,  text:'OPEN'   },
  };
  const { c, text } = map[state] || map.waiting;
  return (
    <div style={{display:'flex', gap:5, alignItems:'center'}}>
      <div style={{width:6, height:6, borderRadius:'50%', background:c}} className={state==='live'?'hz-pulse':undefined}/>
      <span style={{...T.labelSmall, color:'#fff', mixBlendMode:'difference', fontSize:9}}>{text}</span>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────
// Result screen
// ─────────────────────────────────────────────────────────────────────────
function ResultScreen({ game, onReplay, onHome, identityColor }){
  const { score, results } = game;
  const correctCount = results.filter(r=>r==='correct').length;
  const sting = resultStingByScore(score);
  const isWin = score >= 40;
  return (
    <div style={{padding:'18px 20px 24px', position:'relative'}}>
      <Confetti active={isWin} color={identityColor}/>

      <div style={{...T.labelSmall, color:HZ.tx3, marginBottom:8}}>MISSION OUTCOME</div>
      <div style={{...T.headlineMedium, color: isWin?HZ.tx:HZ.magenta, marginBottom:18}}>
        {isWin ? totalToTier(score) : 'FLATLINED'}
      </div>

      <Shelf faceColor={HZ.s2} shelfColor={HZ.s4} rimColor={identityColor} dx={5} dy={5}
        style={{display:'block', width:'100%'}}>
        <div style={{padding:'24px 22px 22px'}}>
          {/* Big score */}
          <div style={{display:'flex', alignItems:'flex-end', justifyContent:'space-between', marginBottom:4}}>
            <div>
              <div style={{...T.labelSmall, color:HZ.tx3}}>SCORE</div>
              <div style={{...T.displayLarge, color:isWin?HZ.cyan:HZ.magenta, fontSize:72, lineHeight:.9}}>
                {score}
              </div>
            </div>
            <div style={{textAlign:'right'}}>
              <div style={{...T.labelSmall, color:HZ.tx3}}>OF 100</div>
              <div style={{...T.headlineSmall, color:HZ.tx2}}>MAX</div>
            </div>
          </div>

          <div style={{height:1, background:HZ.s4, margin:'18px 0'}}/>

          {/* Stats grid */}
          <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:14}}>
            <div>
              <div style={{...T.labelSmall, color:HZ.tx3}}>CORRECT</div>
              <div style={{...T.displaySmall, color:HZ.green, fontSize:30}}>{correctCount}<span style={{color:HZ.tx3, fontSize:18}}>/{TOTAL_ROUNDS}</span></div>
            </div>
            <div>
              <div style={{...T.labelSmall, color:HZ.tx3}}>TIGHTEST ΔE</div>
              <div style={{...T.displaySmall, color:HZ.yellow, fontSize:30}}>0.50</div>
            </div>
            <div>
              <div style={{...T.labelSmall, color:HZ.tx3}}>STREAK</div>
              <div style={{...T.displaySmall, color:HZ.cyan, fontSize:30}}>{longestStreak(results)}</div>
            </div>
            <div>
              <div style={{...T.labelSmall, color:HZ.tx3}}>GEMS</div>
              <div style={{...T.displaySmall, color:HZ.yellow, fontSize:30}}>+{correctCount*2}</div>
            </div>
          </div>

          <div style={{height:1, background:HZ.s4, margin:'18px 0'}}/>

          {/* Round dots recap */}
          <div style={{...T.labelSmall, color:HZ.tx3, marginBottom:8}}>ROUND-BY-ROUND</div>
          <div style={{display:'flex', gap:5}}>
            {results.map((r,i)=>(
              <div key={i} style={{
                flex:1, height:18,
                background: r==='correct'?HZ.green : r==='wrong'?HZ.magenta : HZ.s4,
              }}/>
            ))}
          </div>

          <div style={{height:1, background:HZ.s4, margin:'18px 0'}}/>

          {/* Sting */}
          <div style={{...T.headlineMedium, color:HZ.tx, lineHeight:1.2}}>{sting}</div>
        </div>
      </Shelf>

      <div style={{display:'flex', gap:10, marginTop:18}}>
        <HuezooButton variant="Ghost" full onClick={onHome}>HOME</HuezooButton>
        <HuezooButton variant="Primary" full onClick={onReplay}>PLAY AGAIN</HuezooButton>
      </div>

      <div style={{marginTop:14, display:'flex', justifyContent:'center'}}>
        <HuezooButton variant="Score" dense onClick={()=>{}}>↗ SHARE SCORE CARD</HuezooButton>
      </div>
    </div>
  );
}

function longestStreak(results){
  let m=0, c=0;
  for(const r of results){ if(r==='correct'){c++; m=Math.max(m,c);} else c=0; }
  return m;
}

// ─────────────────────────────────────────────────────────────────────────
// Phone frame (lightweight — matches Huezoo dark canvas, no iOS chrome conflict)
// ─────────────────────────────────────────────────────────────────────────
function HuezooPhone({ children, identity=HZ.cyan, secondary=HZ.magenta }){
  return (
    <div style={{
      width:390, height:844, borderRadius:48, position:'relative',
      background:'#000', overflow:'hidden',
      boxShadow:`0 50px 100px rgba(0,0,0,.55), 0 0 0 8px #1a1a22, 0 0 0 9px #2a2a35`,
    }}>
      {/* dynamic island */}
      <div style={{
        position:'absolute', top:11, left:'50%', transform:'translateX(-50%)',
        width:120, height:34, borderRadius:24, background:'#000', zIndex:50,
      }}/>
      {/* status bar text */}
      <div style={{
        position:'absolute', top:0, left:0, right:0, height:54, zIndex:40,
        display:'flex', alignItems:'center', justifyContent:'space-between',
        padding:'18px 30px 0',
      }}>
        <span style={{color:'#fff', fontWeight:600, fontSize:15, fontFamily:'-apple-system,system-ui'}}>9:41</span>
        <span style={{color:'#fff', fontSize:11, opacity:.8}}>● ● ●</span>
      </div>
      {/* screen */}
      <div className="scroll-hidden" style={{
        position:'absolute', inset:0, paddingTop:54, paddingBottom:34,
        overflow:'auto',
      }}>
        <AmbientGlow primary={identity} secondary={secondary}>
          {children}
        </AmbientGlow>
      </div>
      {/* home indicator */}
      <div style={{
        position:'absolute', bottom:8, left:'50%', transform:'translateX(-50%)',
        width:139, height:5, borderRadius:100, background:'rgba(255,255,255,.7)', zIndex:60,
      }}/>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────
// Annotation tag (engineering callouts pinned next to phone)
// ─────────────────────────────────────────────────────────────────────────
function Annot({ x, y, dir='right', label, body }){
  return (
    <div style={{
      position:'absolute', left:x, top:y, width:260,
      fontFamily:'var(--f-body)',
    }}>
      <div style={{
        background:hexAlpha('#0E0E18',.78), backdropFilter:'blur(8px)',
        border:`1px solid ${hexAlpha(HZ.cyan,.25)}`,
        padding:'10px 12px',
        boxShadow:`0 12px 30px rgba(0,0,0,.4), inset 1px 1px 0 ${hexAlpha(HZ.cyan,.15)}`,
      }}>
        <div style={{...T.labelSmall, color:HZ.cyan, marginBottom:5}}>◉ {label}</div>
        <div style={{...T.bodySmall, color:HZ.tx2, lineHeight:1.45}}>{body}</div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────
// Main App
// ─────────────────────────────────────────────────────────────────────────
const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "variation": "vault",
  "difficulty": "medium",
  "forceState": "play",
  "forceAnswer": "random",
  "showNotes": true,
  "seed": 42
}/*EDITMODE-END*/;

const DIFFICULTY_SCALES = { easy:1.6, medium:1.0, hard:0.65, elite:0.35 };

function App(){
  const [t, setTweak] = useTweaks(TWEAK_DEFAULTS);
  const [seed, setSeed] = React.useState(t.seed);

  // The game engine
  const game = useGame({
    difficultyScale: DIFFICULTY_SCALES[t.difficulty] || 1,
    paused: t.forceState !== 'play',
    forcedAnswer: t.forceAnswer === 'random' ? null : t.forceAnswer,
    seed,
  });

  // Force-state overrides for engineering review
  React.useEffect(()=>{
    if (t.forceState === 'play') return;
    // Stop game timers and force phase
    if (t.forceState === 'memory')   game.jumpTo(PHASES.Memory,   game.round);
    if (t.forceState === 'hold')     game.jumpTo(PHASES.Hold,     game.round);
    if (t.forceState === 'recall')   game.jumpTo(PHASES.Recall,   game.round);
    if (t.forceState === 'correct' || t.forceState === 'wrong') {
      // Force feedback with a manufactured answer
      const correct = t.forceState === 'correct';
      // We need colors to exist; generate them on the fly
      const de = dEForRound(game.round, DIFFICULTY_SCALES[t.difficulty] || 1);
      const c = generateRoundColors({ round: game.round, deltaE: de, isSame: correct, seed: seed + game.round*101 });
      // hack: synthesize feedback state by setting via internal API
      // (real Compose ViewModel exposes setForcedPhase for previews)
      game.jumpTo(PHASES.Memory, game.round); // reset
      setTimeout(()=>{
        game.jumpTo(PHASES.Recall, game.round);
        setTimeout(()=> game.answer(correct), 40);
      }, 30);
    }
    if (t.forceState === 'result') {
      // Synthesize a partly-played session for preview
      game.mockResult();
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t.forceState]);

  const identity = HZ.purple; // Color Memory Match identity color — distinct from Threshold's GameThreshold
  const secondary = HZ.cyan;

  const VariationComp = t.variation === 'twinlock' ? VariationTwinLock : VariationVault;

  return (
    <div style={{
      width:'100%', display:'flex', alignItems:'flex-start', justifyContent:'center',
      gap:48, flexWrap:'wrap', padding:'10px 0 60px',
    }}>
      {/* LEFT: title block */}
      <div style={{maxWidth:300, marginTop:30, color:HZ.tx2}}>
        <div style={{...T.labelSmall, color:HZ.cyan, marginBottom:10}}>HUEZOO · GAME 6 · PROPOSAL</div>
        <h1 style={{...T.headlineLarge, color:HZ.tx, fontSize:46, lineHeight:1.02, margin:'0 0 18px', letterSpacing:'.01em'}}>
          COLOR<br/>MEMORY<br/>MATCH
        </h1>
        <p style={{...T.bodyMedium, color:HZ.tx2, margin:'0 0 20px'}}>
          Color A flashes. Memory chamber seals. Color B appears.
          <strong style={{color:HZ.tx}}> Same — or different?</strong>
        </p>
        <div style={{display:'flex', flexDirection:'column', gap:10}}>
          <SpecLine k="Rounds" v="10"/>
          <SpecLine k="Memory time" v="3.0s"/>
          <SpecLine k="Score" v="+10 correct / −5 wrong"/>
          <SpecLine k="ΔE curve" v="5.0 → 0.5"/>
          <SpecLine k="Identity color" v={<span style={{color:identity}}>AccentPurple #9B5DE5</span>}/>
        </div>

        <div style={{marginTop:28, padding:14, background:hexAlpha(HZ.s2,.6), border:`1px solid ${HZ.s4}`}}>
          <div style={{...T.labelSmall, color:HZ.tx3, marginBottom:6}}>NEW COMPOSE COMPONENTS</div>
          <ul style={{...T.bodySmall, color:HZ.tx2, margin:0, paddingLeft:18, lineHeight:1.6}}>
            <li>CountdownRing.kt (segmented arc, drains over time)</li>
            <li>MemoryChamber.kt (4-state: live/sealed/waiting/revealed)</li>
            <li>CMMatchViewModel.kt + Phase enum</li>
            <li>ChamberShutter.kt (close/open animations)</li>
          </ul>
          <div style={{...T.labelSmall, color:HZ.tx3, margin:'14px 0 6px'}}>REUSES</div>
          <ul style={{...T.bodySmall, color:HZ.tx2, margin:0, paddingLeft:18, lineHeight:1.6}}>
            <li>SkewedStatChip · RoundIndicator · HuezooButton</li>
            <li>ResultCard · AmbientGlowBackground · HuezooTopBar</li>
            <li>ColorEngine (CIEDE2000 ΔE offset)</li>
          </ul>
        </div>
      </div>

      {/* CENTER: phone */}
      <div style={{position:'relative'}}>
        <HuezooPhone identity={identity} secondary={secondary}>
          <HuezooTopBar
            left={<HuezooIconButton variant="Back">{BackChevron}</HuezooIconButton>}
            right={<HuezooIconButton variant="Info">{InfoIcon}</HuezooIconButton>}
          />
          {/* Sub-header strip: small game id + round ΔE on right */}
          <div style={{padding:'12px 20px 0', display:'flex', alignItems:'center', justifyContent:'space-between', gap:12}}>
            <div>
              <div style={{...T.labelSmall, color:identity}}>◉ GAME 6 · MEMORY MATCH</div>
            </div>
            <div style={{flexShrink:0}}>
              <DeltaEBadge value={game.colors?.deltaE} label="ROUND ΔE" align="end"/>
            </div>
          </div>

          {/* identity bar */}
          <div style={{height:3, margin:'14px 20px 0', background:identity}}/>

          {game.phase === PHASES.Result
            ? <ResultScreen game={game} identityColor={identity}
                onReplay={()=>{ game.reset(); setSeed(s=>s+1); }}
                onHome={()=>{ game.reset(); setSeed(s=>s+1); }}
              />
            : <VariationComp game={game}/>
          }
        </HuezooPhone>

        {/* Identity tag */}
        <div style={{position:'absolute', top:-32, left:'50%', transform:'translateX(-50%)', whiteSpace:'nowrap', ...T.labelSmall, color:HZ.tx3}}>
          {t.variation === 'twinlock' ? 'VARIATION B · TWIN LOCK' : 'VARIATION A · THE VAULT'}
        </div>
      </div>

      {/* RIGHT: notes column */}
      {t.showNotes && <NotesColumn variation={t.variation} phase={game.phase}/>}

      {/* Tweaks Panel */}
      <TweaksPanel title="Tweaks">
        <TweakSection label="Variation"/>
        <TweakRadio label="Layout" value={t.variation}
          options={[{value:'vault', label:'A · Vault'},{value:'twinlock', label:'B · Twin'}]}
          onChange={v=> setTweak('variation', v)}/>

        <TweakSection label="Difficulty (ΔE scale)"/>
        <TweakSelect label="Difficulty" value={t.difficulty}
          options={[
            {value:'easy',   label:'Easy (loose ΔE)'},
            {value:'medium', label:'Medium (default)'},
            {value:'hard',   label:'Hard (tight ΔE)'},
            {value:'elite',  label:'Elite (near-human limits)'},
          ]}
          onChange={v=> setTweak('difficulty', v)}/>

        <TweakSection label="State preview"/>
        <TweakSelect label="Force phase" value={t.forceState}
          options={[
            {value:'play',    label:'▶ Play live'},
            {value:'memory',  label:'1 · Memory (A shown)'},
            {value:'hold',    label:'2 · Hold (sealed)'},
            {value:'recall',  label:'3 · Recall (B shown)'},
            {value:'correct', label:'4a · Feedback — correct'},
            {value:'wrong',   label:'4b · Feedback — wrong'},
            {value:'result',  label:'5 · Result screen'},
          ]}
          onChange={v=> setTweak('forceState', v)}/>
        <TweakSelect label="Next answer" value={t.forceAnswer}
          options={[
            {value:'random', label:'Random 50/50'},
            {value:'same',   label:'Always SAME'},
            {value:'diff',   label:'Always DIFFERENT'},
          ]}
          onChange={v=> setTweak('forceAnswer', v)}/>

        <TweakSection label="Display"/>
        <TweakToggle label="Show eng notes" value={t.showNotes}
          onChange={v=> setTweak('showNotes', v)}/>
        <TweakButton label="↻ Restart session" onClick={()=>{ game.reset(); setSeed(s=>s+1); }}/>
      </TweaksPanel>
    </div>
  );
}

function SpecLine({ k, v }){
  return (
    <div style={{display:'flex', justifyContent:'space-between', gap:14, paddingBottom:8, borderBottom:`1px solid ${HZ.s2}`}}>
      <span style={{...T.labelSmall, color:HZ.tx3}}>{k}</span>
      <span style={{...T.bodySmall, color:HZ.tx, textAlign:'right'}}>{v}</span>
    </div>
  );
}

function NotesColumn({ variation, phase }){
  const notes = variation === 'twinlock'
    ? [
        { k:'Memory phase', v:'Chamber A shows Color A live. Chamber B is locked + striped + shows "?". Header: ◉ CHAMBER A LIVE.' },
        { k:'Seal animation', v:'A shutter (vertical scanlines, yellow rim) slides down inside Chamber A over 400ms. Yellow LED + SEAL tag.' },
        { k:'Recall phase', v:'Chamber B unlocks: "?" dissolves, Color B fades in. Buttons enable. Header: ◉ CHAMBER B LIVE — RECALL.' },
        { k:'Feedback', v:'Chamber A shutter slides back up (UNSEALED tag). Player can now compare A and B side by side. Sting copy fires.' },
        { k:'Why this works', v:'Diagnostic / forensic feel. The reveal acts as an "answer key" — players see whether they were right and how close.' },
      ]
    : [
        { k:'Memory phase', v:'One large central swatch. 24-segment ring drains arc-by-arc over 3s. Tactical / scanner feel — matches Threshold ScannerIllustration vocabulary.' },
        { k:'Hold phase', v:'Swatch swaps for a SEALED chamber (diagonal stripes, blinking ◉ SEALED tag). 400ms interstitial — prevents afterimage.' },
        { k:'Recall phase', v:'New swatch (Color B) pops in from a magenta-rimmed scaffold. Buttons unlock.' },
        { k:'Feedback', v:'A and B render side-by-side at smaller scale with = or ≠ glyph. TRUTH label confirms the answer.' },
        { k:'Why this works', v:'Faster, punchier. Single focus point matches Threshold\'s discipline. Best on phones where horizontal space is tight.' },
      ];

  return (
    <div style={{maxWidth:280, marginTop:30, display:'flex', flexDirection:'column', gap:10}}>
      <div style={{...T.labelSmall, color:HZ.cyan}}>◉ DESIGN NOTES — {variation==='twinlock'?'TWIN LOCK':'VAULT'}</div>
      <div style={{...T.bodySmall, color:HZ.tx3, marginBottom:8}}>Current phase: <span style={{color:HZ.tx, ...T.labelSmall}}>{phase.toUpperCase()}</span></div>
      {notes.map((n,i)=>(
        <div key={i} style={{padding:'10px 12px', background:hexAlpha(HZ.s1,.8), border:`1px solid ${HZ.s2}`}}>
          <div style={{...T.labelSmall, color:HZ.tx, marginBottom:5}}>{n.k}</div>
          <div style={{...T.bodySmall, color:HZ.tx2, lineHeight:1.5}}>{n.v}</div>
        </div>
      ))}

      <div style={{marginTop:8, padding:'10px 12px', background:hexAlpha(HZ.s2,.8), border:`1px dashed ${HZ.s4}`}}>
        <div style={{...T.labelSmall, color:HZ.yellow, marginBottom:5}}>STING COPY POOL</div>
        <div style={{...T.bodySmall, color:HZ.tx2, lineHeight:1.55}}>
          Wrong (ΔE&lt;1.0): <em style={{color:HZ.tx}}>"ΔE 0.62. Barely real. Still missed."</em>
          <br/><br/>
          Correct (hard): <em style={{color:HZ.tx}}>"Eyes did not blink."</em>
          <br/><br/>
          Result (≥80): <em style={{color:HZ.tx}}>"Your memory is elite."</em>
        </div>
      </div>
    </div>
  );
}

// Icons (SVG, no emoji)
const BackChevron = (
  <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
    <path d="M14 5L8 11L14 17" stroke="#fff" strokeWidth="2" strokeLinecap="square"/>
  </svg>
);
const InfoIcon = (
  <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
    <circle cx="11" cy="11" r="8.5" stroke="#fff" strokeWidth="1.7"/>
    <rect x="10" y="9" width="2" height="6.5" fill="#fff"/>
    <rect x="10" y="5.5" width="2" height="2" fill="#fff"/>
  </svg>
);

Object.assign(window, { App });
