// Prints what a scenario measures, so a run is self-describing: the summary alone
// does not say which question the numbers answer, and saved output (for example
// perf-results/k6-s1.txt) is read long after the run.
//
// Called from setup(), which executes once. The init context runs in every VU, so a
// banner there would repeat hundreds of times. k6 renders a multi-line message as
// literal "\n" escapes, so every line is logged separately.
//
// Fields:
//   id      short scenario id, matching the file name and the README ("S1")
//   name    what is being loaded, in a few words
//   checks  the question the run answers
//   expects the outcome that means "as designed" — including thresholds that are
//           supposed to fail, so a red run is not mistaken for a broken test
//   load    the knobs actually in effect, built from the env vars at runtime
//   watch   the signals to follow live in monitor.sh, Grafana or the summary
export function logScenario({ id, name, checks, expects, load, watch }) {
  console.log(`${id} — ${name}`);
  console.log(`  checks:  ${checks}`);
  console.log(`  expects: ${expects}`);
  console.log(`  load:    ${load}`);
  console.log(`  watch:   ${watch}`);
}
