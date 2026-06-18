export const roundMatchScore = (score) => {
  const numericScore = Number(score);
  if (!Number.isFinite(numericScore)) return null;

  const clampedScore = Math.max(0, Math.min(numericScore, 100));
  return Math.round(clampedScore / 5) * 5;
};

export const getMatchScoreLevel = (score) => {
  const displayScore = roundMatchScore(score);
  if (displayScore === null) return null;
  if (displayScore >= 80) return 'high';
  if (displayScore >= 60) return 'medium';
  return 'low';
};
