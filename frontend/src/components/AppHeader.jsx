import { Link } from "react-router-dom";

const AppHeader = ({
  brandTo = "/",
  brandInitial = "C",
  brandLabel = "Complaint Register Portal",
  title,
  subtitle,
  actions,
}) => {
  return (
    <header className="app-header">
      <div className="app-header__inner">
        <div className="app-header__left">
          <Link to={brandTo} className="app-header__brand">
            <span className="ui-brand-mark">{brandInitial}</span>
            <span className="app-header__brand-label">{brandLabel}</span>
          </Link>

          {(title || subtitle) && (
            <div className="app-header__context">
              {title && <p className="app-header__title">{title}</p>}
              {subtitle && <p className="app-header__subtitle">{subtitle}</p>}
            </div>
          )}
        </div>

        <div className="app-header__actions">{actions}</div>
      </div>
    </header>
  );
};

export default AppHeader;
