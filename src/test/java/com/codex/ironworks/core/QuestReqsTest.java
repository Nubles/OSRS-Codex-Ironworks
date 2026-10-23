package com.codex.ironworks.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class QuestReqsTest
{
	@Test
	public void everyNameIsOneTheGameKnows() throws Exception
	{
		Set<String> quests = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
		Set<String> skills = Arrays.stream(Skill.values()).map(Skill::getName).collect(Collectors.toSet());
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
			QuestReqs.class.getResourceAsStream("/com/codex/ironworks/quest-reqs.txt"), StandardCharsets.UTF_8)))
		{
			for (String line = r.readLine(); line != null; line = r.readLine())
			{
				if (line.startsWith("#"))
				{
					continue;
				}
				String[] cols = line.split("\\|", -1);
				assertEquals(line, 4, cols.length);
				assertTrue(cols[0], quests.contains(cols[0]));
				QuestReqs.Req req = QuestReqs.parse(Arrays.asList(line)).of(cols[0]);
				for (String q : req.quests)
				{
					assertTrue(line + ": " + q, quests.contains(q));
				}
				for (String s : req.skills.keySet())
				{
					assertTrue(line + ": " + s, skills.contains(s));
				}
			}
		}
	}

	@Test
	public void regicideAsksForUndergroundPassAndAgility()
	{
		QuestReqs.Req r = QuestReqs.load().of("Regicide");
		assertTrue(r.quests.contains("Underground Pass"));
		assertEquals(Integer.valueOf(56), r.skills.get("Agility"));
	}
}
