package com.senacor.hackingdays.distributedcache.db;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.senacor.hackingdays.distributedcache.generate.ProfileGenerator;
import com.senacor.hackingdays.distributedcache.generate.model.Profile;

public class ProfileMapperTest {

private DataSource dataSource;
	private ProfileMapper profileMapper;

	private static DataSource createDataSource() {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:test");
		JdbcConnectionPool pool = JdbcConnectionPool.create(dataSource);
		pool.setMaxConnections(10);
		return pool;
	}

	@Before
	public void initializeConnection() throws ClassNotFoundException, SQLException {
		dataSource = createDataSource();
		profileMapper = new ProfileMapper(dataSource);
	}

	@After
	public void closeConnection() throws SQLException {
		profileMapper = null;
		dataSource.getConnection().createStatement().execute("SHUTDOWN");
	}

	@Test
	public void testCreateProfile() {
		Profile profile = ProfileGenerator.newProfile();
		profileMapper.insertProfile(profile);

		Profile reloadedProfile = profileMapper.getProfileById(profile.getId());
		assertThat(reloadedProfile, is(equalTo(profile)));

		List<Profile> allProfiles = profileMapper.getAllProfiles();
		assertThat(allProfiles.size(), is(equalTo(1)));
	}

	@Test
	public void testCreateManyProfiles() {
		final int samples = 10000;
		ProfileGenerator profileGenerator = ProfileGenerator.newInstance(samples);

		List<Profile> profiles = new ArrayList<>(samples);
		profileGenerator.stream().forEach(profile -> {
			profiles.add(profile);
			profileMapper.insertProfile(profile);
		});

		List<Profile> allProfiles = profileMapper.getAllProfiles();
		assertThat(allProfiles.size(), is(equalTo(samples)));
		assertThat(allProfiles, is(equalTo(profiles)));
	}

	@Test
	public void testDeleteProfile() {
		final int samples = 1000;
		ProfileGenerator profileGenerator = ProfileGenerator.newInstance(samples);

		List<Profile> originalProfiles = new ArrayList<>(samples);
		profileGenerator.stream().forEach(profile -> {
			originalProfiles.add(profile);
			profileMapper.insertProfile(profile);
		});

		List<Profile> allProfiles = profileMapper.getAllProfiles();
		assertThat(allProfiles.size(), is(equalTo(samples)));
		assertThat(allProfiles, is(equalTo(originalProfiles)));

		Profile profile = ProfileGenerator.newProfile();
		profileMapper.insertProfile(profile);

		allProfiles = profileMapper.getAllProfiles();
		assertThat(allProfiles.size(), is(equalTo(samples + 1)));

		profileMapper.deleteProfile(profile.getId());
		allProfiles = profileMapper.getAllProfiles();
		assertThat(allProfiles, is(equalTo(originalProfiles)));
	}

	@Test
	public void testUpdateProfile() {
		Profile profile = ProfileGenerator.newProfile();
		profileMapper.insertProfile(profile);
		Profile reloadedProfile = profileMapper.getProfileById(profile.getId());

		profile.setAge(profile.getAge() + 10);
		assertThat(profile, is(not(equalTo(reloadedProfile))));

		profileMapper.updateProfile(profile);
		reloadedProfile = profileMapper.getProfileById(profile.getId());
		assertThat(reloadedProfile, is(equalTo(profile)));
		assertThat(profileMapper.getAllProfiles().size(), is(equalTo(1)));
	}

	@Test
	public void testGetAllIds() {
		final int samples = 1000;
		ProfileGenerator profileGenerator = ProfileGenerator.newInstance(samples);

		List<Profile> originalProfiles = new ArrayList<>(samples);
		profileGenerator.stream().forEach(profile -> {
			originalProfiles.add(profile);
			profileMapper.insertProfile(profile);
		});

		List<UUID> allProfileIds = profileMapper.getAllIds();

		assertTrue(originalProfiles.stream().allMatch(new Predicate<Profile>() {
			@Override
			public boolean test(Profile t) {
				return allProfileIds.contains(t.getId());
			}
		}));

		assertThat(allProfileIds.size(), equalTo(samples));

	}

}