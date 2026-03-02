
## Language & comments
- Communicate in Chinese. please!!!!
- Code comments MUST be in English (Javadoc + inline comments).

## Java version
- This project targets Java 8. Do not use APIs/language features beyond Java 8.

## Editing workflow
- When modifying code, prefer patch-style edits (minimal diffs).
- Before applying changes, show the exact diff (unified diff) for review.

## New class header (MANDATORY)
When creating ANY new Java class/interface/enum, add this Javadoc at the top of the type:

/**
 * <One-line purpose summary>.
 *
 * @author shentw
 * @version 1.0
 * @since YYYY-MM-DD
 */

Rules:
- @author is ALWAYS "shentw".
- @since is TODAY's date.
- If the agent is unsure of today's date, it MUST read system date from terminal (e.g., `date +%F`) and use that value.
- 如果让你提交git， 永远都要符合git的提交标准； 使用feat\refactor\style\chore\doc\fix这几个关键词， 然后“:”后面紧跟中文描述， 要将明白修改了什么， 新增了什么；
