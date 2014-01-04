/**
 * 
 */
package committools.data;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Commit Walker that visits the commit in a topological order.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public abstract class AbstractCommitWalker {

	public interface ICommitWalkingStrategy {
		public List<RevCommit> getWalk(final Git git) throws Exception;
	}

	/**
	 * Walk all commits topologically.
	 */
	public static final ICommitWalkingStrategy TOPOLOGICAL_WALK = new ICommitWalkingStrategy() {

		@Override
		public List<RevCommit> getWalk(final Git git) throws Exception {
			try {
				return GitCommitUtils.getAllCommitsTopological(git);
			} catch (final NoWorkTreeException e) {
				throw new Exception(e);
			} catch (final NoHeadException e) {
				throw new Exception(e);
			} catch (final IOException e) {
				throw new Exception(e);
			} catch (final GitAPIException e) {
				throw new Exception(e);
			}
		}
	};

	/**
	 * A base walk, going from HEAD back to the first parent.
	 */
	public static final ICommitWalkingStrategy BASE_WALK = new ICommitWalkingStrategy() {

		@Override
		public List<RevCommit> getWalk(final Git git) throws Exception {
			try {
				return GitCommitUtils.getAllBaseCommits(git);
			} catch (final NoWorkTreeException e) {
				throw new Exception(e);
			}
		}
	};

	protected final Git repository;

	private final ICommitWalkingStrategy commitWalkingStrategy;

	private static final Logger LOGGER = Logger
			.getLogger(AbstractCommitWalker.class.getName());

	public AbstractCommitWalker(final String repositoryDirectory,
			final ICommitWalkingStrategy walkingStrategy) throws IOException {
		repository = GitCommitUtils.getGitRepository(repositoryDirectory);
		commitWalkingStrategy = walkingStrategy;
	}

	public void doWalk() {
		doWalk(Integer.MAX_VALUE);
	}

	public void doWalk(final int commitLimit) {
		try {
			final List<RevCommit> allCommits = commitWalkingStrategy
					.getWalk(repository);

			int iteration = 0;
			for (final RevCommit commit : allCommits) {
				if (!vistCommit(commit) || iteration > commitLimit) {
					break;
				}
				iteration++;
			}
		} catch (final NoHeadException e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		} catch (final GitAPIException e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		} catch (final MissingObjectException e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		} catch (final IncorrectObjectTypeException e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		} catch (final IOException e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		} catch (final Exception e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		}
		walkCompleted();
	}

	/**
	 * Visitor implemented by subclasses.
	 * 
	 * @param commit
	 * @return true if visiting should continue
	 */
	public abstract boolean vistCommit(final RevCommit commit);

	/**
	 * Function to be called when the walk is complete.
	 */
	public abstract void walkCompleted();
}