package com.codex.ironworks.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What each quest asks before it can be started: levels, quests finished first, and quest points. */
public final class QuestReqs
{
	/** One quest's requirements. */
	public static final class Req
	{
		public final Map<String, Integer> skills = new LinkedHashMap<>();
		public final List<String> quests = new ArrayList<>();
		public int questPoints;
	}

	private static final Req NONE = new Req();

	private final Map<String, Req> reqs;

	private QuestReqs(Map<String, Req> reqs)
	{
		this.reqs = reqs;
	}

	/** The quest's requirements; empty for a quest with none or one not known. */
	public Req of(String quest)
	{
		return reqs.getOrDefault(quest, NONE);
	}

	public static QuestReqs load()
	{
		try (InputStream in = QuestReqs.class.getResourceAsStream("/com/codex/ironworks/quest-reqs.txt"))
		{
			if (in == null)
			{
				return new QuestReqs(Collections.emptyMap());
			}
			List<String> lines = new ArrayList<>();
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			for (String line = r.readLine(); line != null; line = r.readLine())
			{
				lines.add(line);
			}
			return parse(lines);
		}
		catch (IOException e)
		{
			return new QuestReqs(Collections.emptyMap());
		}
	}

	static QuestReqs parse(List<String> lines)
	{
		Map<String, Req> reqs = new HashMap<>();
		for (String line : lines)
		{
			if (line.isEmpty() || line.startsWith("#"))
			{
				continue;
			}
			String[] cols = line.split("\\|", -1);
			if (cols.length < 4)
			{
				continue;
			}
			Req req = new Req();
			try
			{
				for (String s : cols[1].split(","))
				{
					int space = s.lastIndexOf(' ');
					if (space > 0)
					{
						req.skills.put(s.substring(0, space).trim(), Integer.parseInt(s.substring(space + 1).trim()));
					}
				}
				for (String q : cols[2].split(";"))
				{
					if (!q.trim().isEmpty())
					{
						req.quests.add(q.trim());
					}
				}
				req.questPoints = cols[3].trim().isEmpty() ? 0 : Integer.parseInt(cols[3].trim());
			}
			catch (NumberFormatException ignored)
			{
				// A malformed line asks for what was read before the fault.
			}
			reqs.put(cols[0].trim(), req);
		}
		return new QuestReqs(reqs);
	}
}
